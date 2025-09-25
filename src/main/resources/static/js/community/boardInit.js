$(document).ready(function() {
    // 게시글 작성
    $('.writeBtn').on('click', gotoWritePost);
});

function gotoWritePost() {
    const boardName = $('#boardName').val();

    location.href = `writePost?name=${boardName}`
}