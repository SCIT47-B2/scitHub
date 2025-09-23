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
            alert("検査ワードを入力して下さい。");
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
            console.error('データのロードに失敗しました:', error);
            $('#post-list-body').html('<tr class="no-posts-message"><td colspan="6">データの読込み中エラーが生じました。</td></tr>');
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
        $postListBody.html('<tr class="no-posts-message"><td colspan="6">表示するポストがありません。</td></tr>');
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
 * - 고정된 페이지 블록과 맨 처음/맨 끝 이동 버튼을 포함합니다.
 *
 * @param {Page<PostDTO>} pageData - Spring의 Page 객체
 * @param {number} boardId - 현재 게시판 ID
 * @param {string|null} searchType - 현재 검색 유형
 * @param {string|null} keyword - 현재 검색어
 */
function renderPagination(pageData, boardId, searchType, keyword) {
    const $pagination = $('#pagination-container');
    $pagination.empty();

    // 표시할 데이터가 없으면 종료
    if (!pageData || pageData.totalPages === 0) {
        return;
    }

    // 전체 페이지가 1개뿐인 경우, '1'을 현재 페이지로 표시하고 종료
    if (pageData.totalPages === 1) {
        const $page1 = $('<b>').text(1).addClass('active');
        $pagination.append($page1);
        return;
    }

    const currentPage = pageData.number;      // 현재 페이지 (0-indexed)
    const totalPages = pageData.totalPages;    // 전체 페이지 수
    const pagesPerBlock = 5;                  // 한 블록에 표시할 페이지 수

    // 현재 페이지가 속한 블록의 시작과 끝 페이지를 계산
    const currentBlock = Math.floor(currentPage / pagesPerBlock);
    const startPage = currentBlock * pagesPerBlock;
    const endPage = Math.min(startPage + pagesPerBlock - 1, totalPages - 1);

    // '맨 처음' (<<) 버튼: 현재 페이지가 1, 2가 아닐 때만 표시
    if (currentPage > 1) {
        const $firstLink = $('<a>').html('&laquo;').on('click', () => loadPosts(boardId, 0, searchType, keyword));
        $pagination.append($firstLink);
    }

    // '이전 페이지' (<) 버튼: 첫 페이지가 아닐 때만 표시
    if (currentPage > 0) {
        const $prevPageLink = $('<a>').html('&lsaquo;').on('click', () => loadPosts(boardId, currentPage - 1, searchType, keyword));
        $pagination.append($prevPageLink);
    }

    // 페이지 번호 버튼 렌더링 (블록 단위)
    for (let i = startPage; i <= endPage; i++) {
        const pageNum = i;
        const $pageLink = (pageNum === currentPage)
            ? $('<b>').text(pageNum + 1).addClass('active')
            : $('<a>').text(pageNum + 1).on('click', () => loadPosts(boardId, pageNum, searchType, keyword));
        $pagination.append($pageLink);
    }

    // '다음 페이지' (>) 버튼: 마지막 페이지가 아닐 때만 표시
    if (currentPage < totalPages - 1) {
        const $nextPageLink = $('<a>').html('&rsaquo;').on('click', () => loadPosts(boardId, currentPage + 1, searchType, keyword));
        $pagination.append($nextPageLink);
    }

    // '맨 끝' (>>) 버튼: 마지막 또는 마지막 전 페이지가 아닐 때만 표시
    if (currentPage < totalPages - 2) {
        const $lastLink = $('<a>').html('&raquo;').on('click', () => loadPosts(boardId, totalPages - 1, searchType, keyword));
        $pagination.append($lastLink);
    }
}