"""
부서별 실적 분석 서비스
- DB에서 실적 데이터 조회
- AI로 질문 분석 (부서명, 연도 추출)
- 그래프 생성 (Base64 이미지)
"""
import pymysql
import matplotlib
matplotlib.use('Agg')  # GUI 없이 사용
import matplotlib.pyplot as plt
import matplotlib.font_manager as fm
import io
import base64
import re
from datetime import datetime
from typing import Optional, List, Dict, Any

class PerformanceService:
    def __init__(self, ollama_service=None):
        self.ollama = ollama_service
        self.db_config = {
            'host': 'localhost',
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
        # 유효한 부서 목록
        self.valid_departments = [
            "개발1팀", "개발2팀", "영업팀", "마케팅팀", 
            "인사팀", "재무팀", "기획팀", "디자인팀"
        ]
        # 한글 폰트 설정
        self._setup_font()
    
    def _setup_font(self):
        """한글 폰트 설정"""
        try:
            # Windows
            font_path = 'C:/Windows/Fonts/malgun.ttf'
            font_prop = fm.FontProperties(fname=font_path)
            plt.rcParams['font.family'] = font_prop.get_name()
        except:
            # 기본 폰트
            plt.rcParams['font.family'] = 'Malgun Gothic'
        plt.rcParams['axes.unicode_minus'] = False
    
    def process_query(self, user_prompt: str) -> dict:
        """메인 처리 로직"""
        try:
            # 1. 질문에서 부서명, 연도 추출
            intent = self._parse_intent(user_prompt)
            print(f"[Performance] 파싱 결과: {intent}")
            
            departments = intent.get('departments', [])
            year = intent.get('year', datetime.now().year)
            chart_type = intent.get('chart_type', 'bar')
            
            if not departments:
                return {
                    "ok": False,
                    "message": "부서명을 찾을 수 없습니다. 예: '개발1팀 개발2팀 실적 비교해줘'",
                    "summary": None,
                    "chartImage": None
                }
            
            # 2. DB에서 데이터 조회
            data = self._get_performance_data(departments, year)
            
            if not data:
                return {
                    "ok": False,
                    "message": f"{', '.join(departments)}의 {year}년 실적 데이터가 없습니다.",
                    "summary": None,
                    "chartImage": None
                }
            
            # 3. 요약 텍스트 생성
            summary = self._generate_summary(data, departments, year)
            
            # 4. 그래프 생성
            chart_image = self._generate_chart(data, departments, year, chart_type)
            
            return {
                "ok": True,
                "message": f"{', '.join(departments)} {year}년 실적 비교 분석이 완료되었습니다!",
                "summary": summary,
                "chartImage": chart_image
            }
            
        except Exception as e:
            print(f"[Performance Error] {e}")
            import traceback
            traceback.print_exc()
            return {
                "ok": False,
                "message": f"처리 중 오류 발생: {str(e)}",
                "summary": None,
                "chartImage": None
            }
    
    def _parse_intent(self, prompt: str) -> dict:
        """질문에서 부서명, 연도, 차트 타입 추출"""
        result = {
            'departments': [],
            'year': datetime.now().year,
            'chart_type': 'bar'
        }
        
        # 부서명 추출
        for dept in self.valid_departments:
            # "개발1팀", "개발 1팀", "개발1" 등 다양한 형태 매칭
            patterns = [
                dept,
                dept.replace("팀", ""),
                dept.replace("1", " 1").replace("2", " 2"),
            ]
            for pattern in patterns:
                if pattern in prompt:
                    if dept not in result['departments']:
                        result['departments'].append(dept)
                    break
        
        # 연도 추출
        year_match = re.search(r'(\d{4})년|(\d{2})년', prompt)
        if year_match:
            year_str = year_match.group(1) or year_match.group(2)
            year_num = int(year_str)
            if year_num < 100:
                result['year'] = 2000 + year_num
            else:
                result['year'] = year_num
        
        # 차트 타입 추출
        if '선' in prompt or 'line' in prompt.lower() or '추이' in prompt:
            result['chart_type'] = 'line'
        elif '파이' in prompt or 'pie' in prompt.lower() or '비율' in prompt:
            result['chart_type'] = 'pie'
        else:
            result['chart_type'] = 'bar'
        
        return result
    
    def _get_performance_data(self, departments: List[str], year: int) -> List[Dict]:
        """DB에서 부서별 실적 조회"""
        conn = None
        try:
            print(f"[DB] 연결 시도: {self.db_config['host']}:{self.db_config['port']} (DB: {self.db_config['database']})")
            conn = pymysql.connect(**self.db_config)
            print(f"[DB] ✅ 연결 성공!")
            
            with conn.cursor(pymysql.cursors.DictCursor) as cursor:
                placeholders = ','.join(['%s'] * len(departments))
                sql = f"""
                    SELECT department_name, year, month, 
                           sales_amount, contract_count, 
                           project_count, target_achievement_rate
                    FROM department_performance
                    WHERE department_name IN ({placeholders}) AND year = %s
                    ORDER BY department_name, month
                """
                cursor.execute(sql, (*departments, year))
                result = cursor.fetchall()
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
    
    def _generate_summary(self, data: List[Dict], departments: List[str], year: int) -> str:
        """실적 요약 텍스트 생성"""
        summary_lines = [f"📊 {year}년 부서별 실적 비교\n"]
        summary_lines.append("=" * 40)
        
        for dept in departments:
            dept_data = [d for d in data if d['department_name'] == dept]
            if not dept_data:
                continue
            
            total_sales = sum(d['sales_amount'] for d in dept_data)
            total_contracts = sum(d['contract_count'] for d in dept_data)
            avg_rate = sum(float(d['target_achievement_rate'] or 0) for d in dept_data) / len(dept_data)
            
            summary_lines.append(f"\n🏢 {dept}")
            summary_lines.append(f"   총 매출: {total_sales:,}원 ({total_sales/100000000:.1f}억)")
            summary_lines.append(f"   총 계약: {total_contracts}건")
            summary_lines.append(f"   평균 목표달성률: {avg_rate:.1f}%")
        
        # 비교 분석 (2개 이상일 때)
        if len(departments) >= 2:
            summary_lines.append("\n" + "=" * 40)
            summary_lines.append("\n📈 비교 분석")
            
            dept_totals = {}
            for dept in departments:
                dept_data = [d for d in data if d['department_name'] == dept]
                dept_totals[dept] = sum(d['sales_amount'] for d in dept_data)
            
            sorted_depts = sorted(dept_totals.items(), key=lambda x: x[1], reverse=True)
            summary_lines.append(f"   🥇 매출 1위: {sorted_depts[0][0]} ({sorted_depts[0][1]/100000000:.1f}억)")
            if len(sorted_depts) > 1:
                diff = sorted_depts[0][1] - sorted_depts[1][1]
                summary_lines.append(f"   📊 1위-2위 차이: {diff:,}원 ({diff/10000:.0f}만원)")
        
        return "\n".join(summary_lines)
    
    def _generate_chart(self, data: List[Dict], departments: List[str], 
                       year: int, chart_type: str = 'bar') -> str:
        """그래프 생성 후 Base64 반환"""
        try:
            fig, axes = plt.subplots(1, 2, figsize=(14, 5))
            
            # 데이터 정리
            months = sorted(set(d['month'] for d in data))
            colors = ['#4F46E5', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899', '#06B6D4', '#84CC16']
            
            # ===== 차트 1: 월별 매출 비교 =====
            ax1 = axes[0]
            if chart_type == 'line':
                for i, dept in enumerate(departments):
                    dept_data = [d for d in data if d['department_name'] == dept]
                    sales = [d['sales_amount'] / 10000 for d in sorted(dept_data, key=lambda x: x['month'])]
                    ax1.plot(months[:len(sales)], sales, marker='o', label=dept, 
                            color=colors[i % len(colors)], linewidth=2, markersize=8)
            else:  # bar
                x = range(len(months))
                width = 0.8 / len(departments)
                for i, dept in enumerate(departments):
                    dept_data = [d for d in data if d['department_name'] == dept]
                    sales = [d['sales_amount'] / 10000 for d in sorted(dept_data, key=lambda x: x['month'])]
                    offset = width * (i - len(departments)/2 + 0.5)
                    bars = ax1.bar([xi + offset for xi in x[:len(sales)]], sales, width, 
                                  label=dept, color=colors[i % len(colors)])
            
            ax1.set_xlabel('월', fontsize=12)
            ax1.set_ylabel('매출액 (만원)', fontsize=12)
            ax1.set_title(f'{year}년 월별 매출 비교', fontsize=14, fontweight='bold')
            ax1.set_xticks(range(len(months)))
            ax1.set_xticklabels([f'{m}월' for m in months])
            ax1.legend(loc='upper left')
            ax1.grid(axis='y', alpha=0.3)
            
            # ===== 차트 2: 총 매출 비교 (파이 또는 바) =====
            ax2 = axes[1]
            dept_totals = []
            for dept in departments:
                dept_data = [d for d in data if d['department_name'] == dept]
                total = sum(d['sales_amount'] for d in dept_data)
                dept_totals.append((dept, total))
            
            labels = [d[0] for d in dept_totals]
            values = [d[1] / 100000000 for d in dept_totals]  # 억 단위
            
            if len(departments) <= 3:
                # 파이 차트
                wedges, texts, autotexts = ax2.pie(
                    values, labels=labels, autopct='%1.1f%%',
                    colors=colors[:len(departments)],
                    explode=[0.02] * len(departments),
                    shadow=True
                )
                ax2.set_title(f'{year}년 총 매출 비율', fontsize=14, fontweight='bold')
            else:
                # 수평 바 차트
                y_pos = range(len(labels))
                ax2.barh(y_pos, values, color=colors[:len(departments)])
                ax2.set_yticks(y_pos)
                ax2.set_yticklabels(labels)
                ax2.set_xlabel('매출액 (억원)', fontsize=12)
                ax2.set_title(f'{year}년 총 매출 비교', fontsize=14, fontweight='bold')
                # 값 표시
                for i, v in enumerate(values):
                    ax2.text(v + 0.1, i, f'{v:.1f}억', va='center', fontsize=10)
            
            plt.tight_layout()
            
            # Base64로 변환
            buf = io.BytesIO()
            plt.savefig(buf, format='png', dpi=120, bbox_inches='tight', 
                       facecolor='white', edgecolor='none')
            buf.seek(0)
            img_base64 = base64.b64encode(buf.read()).decode('utf-8')
            plt.close(fig)
            
            return img_base64
            
        except Exception as e:
            print(f"[Chart Error] {e}")
            import traceback
            traceback.print_exc()
            return None

