$(document).ready(function() {
    /**
     * 뒤로가기 버튼 이벤트 처리
     * - 이전 페이지가 글쓰기('/writePost')이면, 해당 게시판의 첫 페이지로 이동
     * - 이전 페이지가 글쓰기('/writePost')가 아니고 이전 페이지 이력이 있다면 , 검색 상태가 유지된 채로 뒤로가기
     */
    $('.back-arrow').click(function(e) {
        e.preventDefault(); // <a> 태그의 기본 링크 이동 동작 방지

        const referrer = document.referrer;
        const boardName = $('#board').val(); // hidden input에서 게시판 이름 가져오기

        // 1. 이전 페이지 URL(referrer) 정보가 있는지 확인
        if (referrer) {
            const previousUrl = new URL(referrer);

            // 이전 페이지가 글쓰기 페이지('/writePost')인 경우
            if (previousUrl.pathname.includes('/writePost') || previousUrl.pathname.includes('/updatePost')) {
                // 해당 게시판의 기본 목록 페이지로 이동
                location.href = `board?name=${boardName}`;
            }
            // 4. 그 외 다른 페이지에서 넘어온 경우
            else {
                // 일반적인 뒤로가기 동작 수행
                history.back();
            }
        }
        // 5. Referrer 정보가 없는 경우 (북마크나 URL 직접 입력으로 접속한 경우)
        else {
            // 기본 동작으로 게시판 목록으로 이동
            location.href = `board?name=${boardName}`;
        }
    });

    // 댓글 목록 가져오기
    loadComments();
    // 태그 목록 표시하기
    displayTags();

    // 댓글 작성
    $('#inputCommentBtn').on('click', inputButtonClick);
    $('#commentBox').on('keydown', function(event) {
        if(event.key === 'Enter') inputButtonClick();
    });
});