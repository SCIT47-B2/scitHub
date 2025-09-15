$('#writePostBtn').click(function() {
    location.href = 'writePost';
});
$('#deletePostBtn').click(function() {
    const flag = confirm("정말로 이 게시글을 삭제하시겠습니까?");
    if(flag) {
        const postId = $('#postId').val();
        location.href = `deletePost?postId=${postId}`;
    }
});
$('#updatePostBtn').click(function() {
    const postId = $('#postId').val();
    location.href = `updatePost?postId=${postId}`;
});