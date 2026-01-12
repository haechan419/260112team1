package com.Team1_Back.repository;

import com.Team1_Back.domain.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {

    @Query("select distinct r from Request r left join fetch r.items order by r.rno desc")
    List<Request> findAllRequests();
    List<Request> findByRequesterOrderByRnoDesc(String requester);

}