package com.Team1_Back.dto;

import lombok.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponseDTO<E> {

    private List<E> content;

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    private boolean hasPrev;
    private boolean hasNext;

    private List<Integer> pageNumList;
    private Integer prevPage;
    private Integer nextPage;

    private List<String> departments;

    public static <E> PageResponseDTO<E> of(
            List<E> content,
            PageRequestDTO request,
            long totalCount
    ) {

        int page = request.getPage();
        int size = request.getSize();
        int totalPages = (int) Math.ceil(totalCount / (double) size);

        int end = (int) (Math.ceil(page / 10.0)) * 10;
        int start = end - 9;
        end = Math.min(end, totalPages);

        boolean hasPrev = start > 1;
        boolean hasNext = totalCount > end * size;

        List<Integer> pageNumList =
                IntStream.rangeClosed(start, end).boxed().collect(Collectors.toList());

        return PageResponseDTO.<E>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalCount)
                .totalPages(totalPages)
                .hasPrev(hasPrev)
                .hasNext(hasNext)
                .pageNumList(pageNumList)
                .prevPage(hasPrev ? start - 1 : null)
                .nextPage(hasNext ? end + 1 : null)
                .build();
    }

    /** Spring Data JPA Page<T> 기반 */
    public static <E> PageResponseDTO<E> from(org.springframework.data.domain.Page<E> page) {
        return PageResponseDTO.<E>builder()
                .content(page.getContent())
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasPrev(page.hasPrevious())
                .hasNext(page.hasNext())
                .build();
    }
}
