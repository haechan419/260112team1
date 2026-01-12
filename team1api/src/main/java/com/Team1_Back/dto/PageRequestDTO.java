package com.Team1_Back.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PageRequestDTO {

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int size = 10;

    private String category;
    private String searchType;
    private String keyword;
    private String department;
    private Boolean isLocked;
    private Boolean isActive;

    public Pageable getPageable(String... props) {
        if (props == null || props.length == 0) {
            return PageRequest.of(page - 1, size);
        }
        return PageRequest.of(page - 1, size, Sort.by(props).descending());
    }

    public int getOffset() {
        return (page - 1) * size;
    }
}
