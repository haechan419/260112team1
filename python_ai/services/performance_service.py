"""
부서별 실적 분석 서비스 (경량화 버전)
- 자연어 질문 분석 (부서명, 연도, 차트 타입 추출)
- 그래프 생성 (Base64 이미지)
"""
import pymysql
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.font_manager as fm
import io
import base64
import re
import json
import requests
from datetime import datetime
from typing import Optional, List, Dict

class PerformanceService:
    def __init__(self, ollama_service=None):
        self.ollama_base_url = "http://127.0.0.1:11434"
        self.ollama_model = "qwen2.5:3b"
        self.db_config = {
            'host': '127.0.0.1', 
            'port': 3306,
            'user': 'root', 
            'password': '1234',
            'database': 'team1db', 
            'charset': 'utf8mb4',
            'connect_timeout': 10,  # 연결 타임아웃 (초)
            'read_timeout': 10,      # 읽기 타임아웃 (초)
            'write_timeout': 10,     # 쓰기 타임아웃 (초)
            'autocommit': True       # 자동 커밋
        }
        self.valid_departments = ["개발1팀", "개발2팀", "영업팀", "마케팅팀", "인사팀", "재무팀", "기획팀", "디자인팀"]
        self._setup_font()
    
    def _setup_font(self):
        try:
            font_prop = fm.FontProperties(fname='C:/Windows/Fonts/malgun.ttf')
            plt.rcParams['font.family'] = font_prop.get_name()
        except:
            plt.rcParams['font.family'] = 'Malgun Gothic'
        plt.rcParams['axes.unicode_minus'] = False
    
    def process_query(self, user_prompt: str) -> dict:
        """메인 처리"""
        try:
            print(f"[Performance] 질문: {user_prompt}")
            
            # 1. 질문 분석 (정규식 우선, Ollama 보조)
            intent = self._parse_intent(user_prompt)
            print(f"[Performance] 분석: {intent}")
            
            departments = intent.get('departments', [])
            year = intent.get('year', datetime.now().year)
            compare_year = intent.get('compare_year')
            chart_type = intent.get('chart_type', 'bar')
            query_type = intent.get('query_type', 'compare')
            
            # 순위 질문이면 전체 부서
            if not departments and query_type in ['ranking', 'all']:
                departments = self.valid_departments
            
            if not departments:
                return {"ok": False, "message": "부서명을 찾을 수 없습니다. 예: '개발1팀 영업팀 비교해줘'", "summary": None, "chartImage": None}
            
            # 2. DB 조회
            data = self._get_data(departments, year)
            compare_data = self._get_data(departments, compare_year) if compare_year else None
            
            if not data:
                return {"ok": False, "message": f"{', '.join(departments)}의 {year}년 데이터가 없습니다.", "summary": None, "chartImage": None}
            
            # 3. 요약 + 차트 생성
            summary = self._generate_summary(data, departments, year, compare_data, compare_year)
            chart = self._generate_chart(data, departments, year, chart_type, compare_data, compare_year)
            
            return {"ok": True, "message": f"{year}년 실적 분석 완료!", "summary": summary, "chartImage": chart}
            
        except Exception as e:
            print(f"[Error] {e}")
            return {"ok": False, "message": f"오류: {str(e)}", "summary": None, "chartImage": None}
    
    def _parse_intent(self, prompt: str) -> dict:
        """질문 분석 (정규식 기반 + Ollama 보조)"""
        result = {'departments': [], 'year': datetime.now().year, 'compare_year': None, 'chart_type': 'bar', 'query_type': 'compare'}
        
        # 부서 추출
        for dept in self.valid_departments:
            if dept in prompt or dept.replace("팀", "") in prompt:
                if dept not in result['departments']:
                    result['departments'].append(dept)
        
        # 연도 추출
        year_match = re.search(r'(\d{4})년|(\d{2})년', prompt)
        if year_match:
            year_str = year_match.group(1) or year_match.group(2)
            year_num = int(year_str)
            result['year'] = 2000 + year_num if year_num < 100 else year_num
        
        # 작년 대비
        if '작년' in prompt or '전년' in prompt:
            result['compare_year'] = result['year'] - 1
            result['query_type'] = 'year_compare'
        
        # 순위/전체 질문
        if any(kw in prompt for kw in ['1위', '최고', '가장', '제일', '순위']):
            result['query_type'] = 'ranking'
        if any(kw in prompt for kw in ['전체', '모든', '전부']):
            result['query_type'] = 'all'
        
        # 차트 타입
        if any(kw in prompt for kw in ['추이', '변화', '트렌드']):
            result['chart_type'] = 'line'
        elif any(kw in prompt for kw in ['비율', '점유']):
            result['chart_type'] = 'pie'
        
        # 부서 없으면 Ollama로 보조 분석
        if not result['departments']:
            ollama_result = self._ollama_parse(prompt)
            if ollama_result.get('query_type'):
                result['query_type'] = ollama_result['query_type']
        
        return result
    
    def _ollama_parse(self, prompt: str) -> dict:
        """Ollama 보조 분석 (간단한 의도 파악만)"""
        try:
            system = f"""질문 의도를 JSON으로 응답. query_type만 반환.
ranking: 순위질문(1위,최고,가장)
compare: 부서비교
trend: 추이분석
all: 전체부서
예: "제일 잘한 팀?" → {{"query_type":"ranking"}}
JSON만 응답."""
            
            res = requests.post(f"{self.ollama_base_url}/api/generate",
                json={"model": self.ollama_model, "prompt": f"{system}\n질문:{prompt}\nJSON:", "stream": False, "options": {"num_predict": 50}},
                timeout=10)
            
            text = res.json().get("response", "")
            match = re.search(r'\{[^}]+\}', text)
            if match:
                return json.loads(match.group())
        except Exception as e:
            print(f"[Ollama] {e}")
        return {}
    
    def _get_data(self, departments: List[str], year: int) -> List[Dict]:
        """DB 조회"""
        if not year:
            return []
        conn = None
        try:
            print(f"[DB] 연결 시도: {self.db_config['host']}:{self.db_config['port']} (DB: {self.db_config['database']})")
            conn = pymysql.connect(**self.db_config)
            print(f"[DB] ✅ 연결 성공!")
            
            with conn.cursor(pymysql.cursors.DictCursor) as cur:
                ph = ','.join(['%s'] * len(departments))
                cur.execute(f"SELECT * FROM department_performance WHERE department_name IN ({ph}) AND year=%s ORDER BY department_name, month", (*departments, year))
                result = cur.fetchall()
                print(f"[DB] ✅ 조회 성공: {len(result)}건")
                return result
        except pymysql.OperationalError as e:
            error_code = e.args[0] if e.args else None
            error_msg = e.args[1] if len(e.args) > 1 else str(e)
            print(f"[DB Error] 연결 오류 (코드: {error_code}): {error_msg}")
            
            if error_code == 2003:
                print(f"[DB Error] 해결 방법:")
                print(f"  1. MariaDB 서버가 실행 중인지 확인: net start MariaDB (관리자 권한 필요)")
                print(f"  2. 포트 {self.db_config['port']}가 열려있는지 확인: netstat -an | findstr {self.db_config['port']}")
                print(f"  3. host를 '127.0.0.1'로 설정했는지 확인 (현재: {self.db_config['host']})")
            elif error_code == 1045:
                print(f"[DB Error] 인증 실패: 사용자명 '{self.db_config['user']}' 또는 비밀번호 확인 필요")
            elif error_code == 1049:
                print(f"[DB Error] 데이터베이스 '{self.db_config['database']}'가 존재하지 않습니다")
                print(f"  사용 가능한 데이터베이스를 확인하세요: SHOW DATABASES;")
            
            return []
        except pymysql.Error as e:
            print(f"[DB Error] MySQL/MariaDB 오류: {e}")
            return []
        except Exception as e:
            print(f"[DB Error] 예상치 못한 오류: {type(e).__name__}: {e}")
            import traceback
            traceback.print_exc()
            return []
        finally:
            if conn: 
                conn.close()
                print(f"[DB] 연결 종료")
    
    def _generate_summary(self, data: List[Dict], departments: List[str], year: int,
                          compare_data: Optional[List[Dict]], compare_year: Optional[int]) -> str:
        """요약 생성"""
        lines = [f"📊 {year}년 부서별 실적\n" + "=" * 35]
        
        # 부서별 통계
        dept_totals = {}
        for dept in departments:
            dept_data = [d for d in data if d['department_name'] == dept]
            if not dept_data:
                continue
            total = sum(d['sales_amount'] for d in dept_data)
            contracts = sum(d['contract_count'] for d in dept_data)
            avg_rate = sum(float(d['target_achievement_rate'] or 0) for d in dept_data) / len(dept_data)
            dept_totals[dept] = total
            
            lines.append(f"\n🏢 {dept}")
            lines.append(f"   매출: {total/100000000:.1f}억 | 계약: {contracts}건 | 달성률: {avg_rate:.1f}%")
            
            if compare_data and compare_year:
                prev = [d for d in compare_data if d['department_name'] == dept]
                if prev:
                    prev_total = sum(d['sales_amount'] for d in prev)
                    growth = (total - prev_total) / prev_total * 100 if prev_total else 0
                    lines.append(f"   {'📈' if growth > 0 else '📉'} 전년대비: {growth:+.1f}%")
        
        # 순위
        if len(dept_totals) >= 2:
            lines.append("\n" + "=" * 35 + "\n📈 순위")
            sorted_depts = sorted(dept_totals.items(), key=lambda x: x[1], reverse=True)
            medals = ["🥇", "🥈", "🥉", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣"]
            for i, (dept, total) in enumerate(sorted_depts[:8]):
                lines.append(f"   {medals[i]} {dept}: {total/100000000:.1f}억")
        
        # AI 인사이트 (부서 4개 이하만)
        if len(departments) <= 4:
            insight = self._get_ai_insight(dept_totals, year)
            if insight:
                lines.append("\n" + "=" * 35)
                lines.append("\n🤖 AI 분석")
                lines.append(insight)
        
        return "\n".join(lines)
    
    def _get_ai_insight(self, dept_totals: Dict, year: int) -> str:
        """간단한 AI 인사이트 생성"""
        try:
            # 데이터 요약
            summary = ", ".join([f"{d}:{t/100000000:.1f}억" for d, t in dept_totals.items()])
            
            prompt = f"""부서 실적 데이터를 보고 한줄 인사이트를 작성해.
데이터: {summary} ({year}년)
형식: ✅강점: (한줄) ⚠️주의: (한줄) 💡제안: (한줄)
3줄 이내, 한국어로."""

            res = requests.post(f"{self.ollama_base_url}/api/generate",
                json={"model": self.ollama_model, "prompt": prompt, "stream": False, "options": {"num_predict": 150}},
                timeout=15)
            
            return res.json().get("response", "").strip()
        except Exception as e:
            print(f"[AI Insight] {e}")
            return ""
    
    def _generate_chart(self, data: List[Dict], departments: List[str], year: int, 
                       chart_type: str, compare_data: Optional[List[Dict]], compare_year: Optional[int]) -> str:
        """차트 생성"""
        try:
            fig, axes = plt.subplots(1, 2, figsize=(14, 5))
            months = sorted(set(d['month'] for d in data))
            colors = ['#4F46E5', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899', '#06B6D4', '#84CC16']
            
            # 차트 1: 월별
            ax1 = axes[0]
            if chart_type == 'line':
                for i, dept in enumerate(departments[:6]):
                    dept_data = sorted([d for d in data if d['department_name'] == dept], key=lambda x: x['month'])
                    sales = [d['sales_amount'] / 10000 for d in dept_data]
                    ax1.plot(months[:len(sales)], sales, marker='o', label=dept, color=colors[i % len(colors)], linewidth=2)
            else:
                x = range(len(months))
                width = 0.8 / min(len(departments), 6)
                for i, dept in enumerate(departments[:6]):
                    dept_data = sorted([d for d in data if d['department_name'] == dept], key=lambda x: x['month'])
                    sales = [d['sales_amount'] / 10000 for d in dept_data]
                    offset = width * (i - min(len(departments), 6)/2 + 0.5)
                    ax1.bar([xi + offset for xi in x[:len(sales)]], sales, width, label=dept, color=colors[i % len(colors)])
            
            ax1.set_xlabel('월')
            ax1.set_ylabel('매출액 (만원)')
            ax1.set_title(f'{year}년 월별 매출 {"추이" if chart_type == "line" else "비교"}', fontweight='bold')
            ax1.set_xticks(range(len(months)))
            ax1.set_xticklabels([f'{m}월' for m in months])
            ax1.legend(loc='upper left', fontsize=8)
            ax1.grid(axis='y', alpha=0.3)
            
            # 차트 2: 총 매출 순위
            ax2 = axes[1]
            dept_totals = [(dept, sum(d['sales_amount'] for d in data if d['department_name'] == dept)) for dept in departments]
            dept_totals.sort(key=lambda x: x[1], reverse=True)
            labels = [d[0] for d in dept_totals[:8]]
            values = [d[1] / 100000000 for d in dept_totals[:8]]
            
            if compare_data and compare_year:
                # 연도 비교
                curr_vals = values
                prev_vals = [sum(d['sales_amount'] for d in compare_data if d['department_name'] == dept) / 100000000 for dept in labels]
                x = range(len(labels))
                ax2.bar([xi - 0.175 for xi in x], prev_vals, 0.35, label=f'{compare_year}년', color='#94A3B8')
                ax2.bar([xi + 0.175 for xi in x], curr_vals, 0.35, label=f'{year}년', color='#4F46E5')
                ax2.set_title(f'{compare_year}년 vs {year}년', fontweight='bold')
                ax2.legend()
            elif len(departments) <= 4:
                # 파이 차트
                ax2.pie(values, labels=labels, autopct='%1.1f%%', colors=colors[:len(labels)], explode=[0.02]*len(labels))
                ax2.set_title(f'{year}년 매출 비율', fontweight='bold')
            else:
                # 수평 바
                ax2.barh(range(len(labels)), values, color=colors[:len(labels)])
                ax2.set_yticks(range(len(labels)))
                ax2.set_yticklabels(labels)
                ax2.set_xlabel('매출액 (억원)')
                ax2.set_title(f'{year}년 매출 순위', fontweight='bold')
                for i, v in enumerate(values):
                    ax2.text(v + 0.1, i, f'{v:.1f}억', va='center')
            
            ax2.set_xticks(range(len(labels))) if compare_data else None
            ax2.set_xticklabels(labels, rotation=45, ha='right') if compare_data else None
            ax2.grid(axis='y', alpha=0.3) if compare_data or len(departments) > 4 else None
            
            plt.tight_layout()
            buf = io.BytesIO()
            plt.savefig(buf, format='png', dpi=120, bbox_inches='tight', facecolor='white')
            buf.seek(0)
            result = base64.b64encode(buf.read()).decode('utf-8')
            plt.close(fig)
            return result
        except Exception as e:
            print(f"[Chart] {e}")
            return None
