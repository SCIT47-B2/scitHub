$(document).ready(function () {
    // hidden input 영역에서 boardId 추출
    const initialBoardId = $('#boardId').val();

    // 1. 페이지 최초 로드
    loadPosts(initialBoardId, 0);
    translateBoardNameToJp();

    // 2. 검색 버튼 클릭 이벤트
    $('#search-btn').on('click', function () {
        const searchType = $('#searchType').val();
        const keyword = $('#search-keyword').val();

        if (keyword.trim() === "") {
            alert("검색어를 입력해주세요.");
            return;
        }

        // 검색 실행 시, 첫 페이지부터 조회
        loadPosts(initialBoardId, 0, searchType, keyword);
    });
});

/**
 * 게시판 이름을 일본어로 해석
 */
function translateBoardNameToJp() {
    const boardName = $('.boardName').text();
    let result = '';
    switch (boardName) {
            case "free":
                result = "自由掲示板";
                break;
            case "it":
                result = "IT情報";
                break;
            case "japanese":
                result = "日本語情報";
                break;
            case "jpCulture":
                result = "日本文化&生活情報";
                break;
            case "job":
                result = "就活情報&コツ";
                break;
            case "hobby":
                result = "趣味&旅行&グルメ情報";
                break;
            case "certificate":
                result = "資格情報";
                break;
            case "graduated":
                result = "卒業生掲示板";
                break;
            default:
                result = "掲示板の名前が間違っています";
                break;
        }
    console.log(result);
    $('.boardName').text(result);
}

/**
 * 게시글 목록 또는 검색 결과를 비동기적으로 로드
 * @param {number} boardId - 게시판 ID
 * @param {number} page - 페이지 번호
 * @param {string|null} searchType - 검색 유형 (예: 'title', 'author')
 * @param {string|null} keyword - 검색어
 */
function loadPosts(boardId, page = 0, searchType = null, keyword = null) {
    let requestData = {
        boardId: boardId,
        page: page,
        size: 10,
        sort: 'createdAt,desc'
    };

    // 검색 파라미터가 존재하면 data 객체에 추가
    if (searchType && keyword) {
        requestData.searchType = searchType;
        requestData.keyword = keyword;
    }

    $.ajax({
        url: `getBoard`, // 동일한 엔드포인트 사용
        type: 'GET',
        data: requestData,
        dataType: 'json',
        success: function (pageData) {
            renderPosts(pageData.content);
            // 페이지네이션에도 검색 상태를 전달
            renderPagination(pageData, boardId, searchType, keyword);
        },
        error: function (xhr, status, error) {
            console.error('데이터 로드 실패:', error);
            $('#post-list-body').html('<tr class="no-posts-message"><td colspan="6">데이터를 불러오는 중 오류가 발생했습니다.</td></tr>');
        }
    });
}

/**
 * 게시글 데이터를 렌더링합니다. (이전과 동일)
 */
function renderPosts(posts) {
    const $postListBody = $('#post-list-body');
    $postListBody.empty();

    if (!posts || posts.length === 0) {
        $postListBody.html('<tr class="no-posts-message"><td colspan="6">표시할 게시글이 없습니다.</td></tr>');
        return;
    }

    $.each(posts, function (index, post) {
        const postRow = `
            <tr>
                <td class="post-title">
                    <span class="postLink"
                        onclick="location.href='readPost?postId=${post.postId}'">
                        ${post.title}
                    </span>
                </td>
                <td class="post-writer">${post.username}</td>
                <td class="post-createdAt">${new Date(post.createdAt).toLocaleDateString()}</td>
                <td class="post-viewCount">${post.viewCount}</td>
                <td class="post-likeCount">${post.likeCount}</td>
                <td class="post-commentCount">${post.commentCount}</td>
            </tr>
        `;
        $postListBody.append(postRow);
    });
}

/**
 * 페이지네이션 UI 생성
 * - 현재 페이지의 앞뒤 2개씩 렌더링
 * - 맨 앞/뒤에서 두 번째 페이지일 경우, 이전/다음 버튼만 각각 표시
 *
 * @param {Page<PostDTO>} pageData - Spring의 Page 객체
 * @param {number} boardId - 현재 게시판 ID
 * @param {string|null} searchType - 현재 검색 유형
 * @param {string|null} keyword - 현재 검색어
 */
function renderPagination(pageData, boardId, searchType, keyword) {
    const $pagination = $('#pagination-container');
    $pagination.empty();

    if (!pageData || pageData.totalPages == 0) { // 페이지가 1개 이하면
        // 페이지가 없으면 종료
        return;
    }

    if (pageData.totalPages == 1) {
        // 1개면 1만 렌더링
        const $page1 = $('<b>').text(1).addClass('active')
        $pagination.append($page1);
        return;
    }

    const currentPage = pageData.number;       // 현재 페이지 (0-indexed)
    const totalPages = pageData.totalPages;    // 전체 페이지 수

    // '맨 처음'과 '이전' 버튼 렌더링 조건
    if (currentPage > 0) { // 첫 페이지가 아닐 때
        // 현재 페이지가 1(두 번째 페이지)일 때는 '이전' 버튼만 표시
        if (currentPage > 1) {
            const $firstLink = $('<a>').html('&laquo;').on('click', () => loadPosts(boardId, 0, searchType, keyword));
            $pagination.append($firstLink);
        }
        const $prevLink = $('<a>').html('&lsaquo;').on('click', () => loadPosts(boardId, currentPage - 1, searchType, keyword));
        $pagination.append($prevLink);
    }

    // 페이지 번호 버튼 렌더링
    let startPage = Math.max(0, currentPage - 2);
    let endPage = Math.min(totalPages - 1, currentPage + 2);

    if (currentPage < 2) {
        endPage = Math.min(totalPages - 1, 4);
    }
    if (currentPage > totalPages - 3) {
        startPage = Math.max(0, totalPages - 5);
    }

    for (let i = startPage; i <= endPage; i++) {
        const pageNum = i;
        const $pageLink = (pageNum === currentPage)
            ? $('<b>').text(pageNum + 1).addClass('active')
            : $('<a>').text(pageNum + 1).on('click', () => loadPosts(boardId, pageNum, searchType, keyword));
        $pagination.append($pageLink);
    }

    // '다음'과 '맨 끝' 버튼 렌더링 조건
    if (currentPage < totalPages - 1) { // 마지막 페이지가 아닐 때
        // 현재 페이지가 마지막에서 두 번째 페이지일 때는 '다음' 버튼만 표시
        if (currentPage < totalPages - 2) {
            const $lastLink = $('<a>').html('&raquo;').on('click', () => loadPosts(boardId, totalPages - 1, searchType, keyword));
            $pagination.append($lastLink);
        }
        const $nextLink = $('<a>').html('&rsaquo;').on('click', () => loadPosts(boardId, currentPage + 1, searchType, keyword));
        $pagination.append($nextLink);
    }
}