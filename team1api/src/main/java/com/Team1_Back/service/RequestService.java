package com.Team1_Back.service;

import com.Team1_Back.dto.RequestDTO;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Transactional
public interface RequestService {

    // 결재 요청 등록
    Long register(RequestDTO requestDTO);

    // 결재 요청 목록 조회
    List<RequestDTO> getList();
    void modifyStatus(Long rno, String status, String rejectReason);
    List<RequestDTO> getListByRequester(String requester);

}

