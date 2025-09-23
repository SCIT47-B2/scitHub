$(document).ready(function() {
    // 뒤로 가기 버튼
    $('.back-arrow').click(function() {
        const board = $('#board').val();
        location.href = `board?name=${board}`
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