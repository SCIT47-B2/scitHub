$(document).ready(function() {
    // 댓글 목록 가져오기
    loadComments();
    // 태그 목록 표시하기
    displayTags();

    // 댓글 작성
    $('#inputCommentBtn').on('click', inputButtonClick);
});