$(document).ready(function () {
    const initialBoardId = $('#boardId').val();

    // 1. 페이지 로드 시 URL에서 1-based 페이지 번호 복원
    const urlParams = new URLSearchParams(window.location.search);
    const searchType = urlParams.get('searchType');
    const keyword = urlParams.get('keyword');

    // URL의 page 파라미터는 1-based. 없으면 1페이지.
    const pageFromUrl = parseInt(urlParams.get('page')) || 1;
    // 서버에 보낼 때는 0-based 인덱스로 변환
    const pageForServer = pageFromUrl - 1;

    let sort = $('#sortType').val() ? $('#sortType').val() : 'createdAt,desc';

    if (searchType && keyword) {
        // 검색창에 값 복원
        $('#searchType').val(searchType);
        $('#search-keyword').val(keyword);
        // 저장된 조건으로 게시글 로드
        loadPosts(initialBoardId, pageForServer, searchType, keyword, sort);
    } else {
        // 기본 게시글 목록 로드
        loadPosts(initialBoardId, pageForServer, sort);
    }

    translateBoardNameToJp();

    // 2. 검색 버튼 및 엔터 키 이벤트
    $('#search-btn').on('click', function() {
        executeSearch(initialBoardId);
    });

    $('#search-keyword').on('keydown', function(event) {
        if (event.key === 'Enter') {
            event.preventDefault();
            executeSearch(initialBoardId);
        }
    });

    $('#sortType').on('change', function() {
        executeSort(initialBoardId);
    });
});


/**
 * 검색을 실행하고 URL을 업데이트하는 함수
 */
function executeSearch(boardId) {
    const searchType = $('#searchType').val();
    const keyword = $('#search-keyword').val();

    if (keyword.trim() === '') {
        alert('検査ワードを入力して下さい。');
        return;
    }
    
    // 검색 시 항상 서버에는 첫 페이지(0)를 요청하고, URL에는 1페이지로 표시
    loadPosts(boardId, 0, searchType, keyword);
}

function executeSort(boardId) {
    const searchType = $('#searchType').val();
    const keyword = $('#search-keyword').val();
    const sort = $('#sortType').val();
    
    // 검색 시 항상 서버에는 첫 페이지(0)를 요청하고, URL에는 1페이지로 표시
    loadPosts(boardId, 0, searchType, keyword, sort);
}

/**
 * URL의 쿼리 스트링을 업데이트하는 함수 (1-based page)
 * @param {number} userPage - 사용자에게 표시될 1-based 페이지 번호
 */
function updateURL(userPage, searchType = null, keyword = null) {
    const url = new URL(window.location);
    // URL에는 항상 1-based 페이지 번호를 설정
    url.searchParams.set('page', userPage);

    if (searchType && keyword) {
        url.searchParams.set('searchType', searchType);
        url.searchParams.set('keyword', keyword);
    } else {
        url.searchParams.delete('searchType');
        url.searchParams.delete('keyword');
    }
    window.history.pushState({ path: url.href }, '', url.href);
}


/**
 * 게시글 목록 또는 검색 결과를 비동기적으로 로드 (0-based page)
 * @param {number} boardId - 게시판 ID
 * @param {number} pageIndex - 서버에 요청할 0-based 페이지 인덱스
 */
function loadPosts(boardId, pageIndex = 0, searchType = null, keyword = null, sort = null) {
    // URL을 업데이트할 때는 1-based 페이지 번호로 변환
    updateURL(pageIndex + 1, searchType, keyword);

    const sortArray = sort ? [sort, 'createdAt,desc', 'postId,desc'] : ['createdAt,desc', 'postId,desc'];

    let requestData = {
        boardId: boardId,
        page: pageIndex, // 서버에는 0-based 인덱스 전송
        size: 10,
        sort: sortArray
    };

    console.log(requestData);

    if (searchType && keyword) {
        requestData.searchType = searchType;
        requestData.keyword = keyword;
    }

    $.ajax({
        url: 'getBoard',
        type: 'GET',
        data: requestData,
        traditional: true,
        dataType: 'json',
        success: function (pageData) {
            renderPosts(pageData.content);
            renderPagination(pageData, boardId, searchType, keyword, sort);
        },
        error: function (xhr, status, error) {
            console.error('データのロードに失敗しました:', error);
            $('#post-list-body').html('<tr class="no-posts-message"><td colspan="6">게시글을 불러오는 데 실패했습니다.</td></tr>');
        }
    });
}

// renderPosts, renderPagination, translateBoardNameToJp 함수는 이전과 동일하게 유지됩니다.

function translateBoardNameToJp() {
    const boardName = $('.boardName').text();
    let result = '';
    switch (boardName) {
        case "free": result = "自由掲示板"; break;
        case "it": result = "IT情報"; break;
        case "japanese": result = "日本語情報"; break;
        case "jpCulture": result = "日本文化&生活情報"; break;
        case "job": result = "就活情報&コツ"; break;
        case "hobby": result = "趣味&旅行&グルメ情報"; break;
        case "certificate": result = "資格情報"; break;
        case "graduated": result = "卒業生掲示板"; break;
        case "qna": result = "Q&A掲示板"; break;
        default: result = "掲示板"; break;
    }
    $('.boardName').text(result);
}

function renderPosts(posts) {
    const $postListBody = $('#post-list-body');
    $postListBody.empty();

    if (!posts || posts.length === 0) {
        $postListBody.html('<tr class="no-posts-message"><td colspan="6">ポストがありません.</td></tr>');
        return;
    }

    $.each(posts, function (index, post) {
        const detailUrl = `readPost?postId=${post.postId}`;

        const postRow = `
            <tr>
                <td class="post-title">
                    <a class="postLink" href="${detailUrl}">${post.title}</a>
                </td>
                <td class="post-writer">${post.userNameKor}</td>
                <td class="post-createdAt">${new Date(post.createdAt).toLocaleDateString()}</td>
                <td class="post-viewCount">${post.viewCount}</td>
                <td class="post-likeCount">${post.likeCount}</td>
                <td class="post-commentCount">${post.commentCount}</td>
            </tr>`;
        $postListBody.append(postRow);
    });
}

function renderPagination(pageData, boardId, searchType, keyword, sort) {
    const $pagination = $('#pagination-container');
    $pagination.empty();

    if (!pageData || pageData.totalPages === 0) {
        return;
    }
    if (pageData.totalPages === 1) {
        const page1 = $('<b>').text('1').addClass('active');
        $pagination.append(page1);
        return;
    }

    const currentPage = pageData.number;
    const totalPages = pageData.totalPages;
    const pagesPerBlock = 5;
    const currentBlock = Math.floor(currentPage / pagesPerBlock);
    const startPage = currentBlock * pagesPerBlock;
    const endPage = Math.min(startPage + pagesPerBlock - 1, totalPages - 1);

    if (currentPage > 0) {
        const firstLink = $('<a>').html('&laquo;').on('click', () => loadPosts(boardId, 0, searchType, keyword, sort));
        $pagination.append(firstLink);
        const prevPageLink = $('<a>').html('&lsaquo;').on('click', () => loadPosts(boardId, currentPage - 1, searchType, keyword, sort));
        $pagination.append(prevPageLink);
    }

    for (let i = startPage; i <= endPage; i++) {
        const pageNum = i;
        const pageLink = (pageNum === currentPage) ?
            $('<b>').text(pageNum + 1).addClass('active') :
            $('<a>').text(pageNum + 1).on('click', () => loadPosts(boardId, pageNum, searchType, keyword, sort));
        $pagination.append(pageLink);
    }

    if (currentPage < totalPages - 1) {
        const nextPageLink = $('<a>').html('&rsaquo;').on('click', () => loadPosts(boardId, currentPage + 1, searchType, keyword, sort));
        $pagination.append(nextPageLink);
        const lastLink = $('<a>').html('&raquo;').on('click', () => loadPosts(boardId, totalPages - 1, searchType, keyword, sort));
        $pagination.append(lastLink);
    }
}