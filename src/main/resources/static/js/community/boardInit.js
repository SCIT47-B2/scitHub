$(document).ready(function() {
    // 댓글 작성
    $('.writeBtn').on('click', gotoWritePost);
});

function gotoWritePost() {
    const boardId = $('#boardId').val();

    location.href = `writePost?boardId=${boardId}`
}