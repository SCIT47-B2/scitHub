$(document).ready(function() {
    // 댓글 목록 가져오기
    commentList();

    // 댓글 작성
    $('#inputCommentBtn').on('click', inputButtonClick);

    // 각 댓글 수정/삭제
    $('#commentTbody').on('click', '.deleteButton', deleteFunc);
    $('#commentTbody').on('click', '.updateButton', updateFunc);
});