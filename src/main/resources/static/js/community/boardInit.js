$(document).ready(function() {
    // 게시글 작성
    $('.writeBtn').on('click', gotoWritePost);
});

function gotoWritePost() {
    const boardId = $('#boardId').val();

    location.href = `writePost?boardId=${boardId}`
}