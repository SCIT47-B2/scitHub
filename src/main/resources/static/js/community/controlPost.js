$('#writePostBtn').click(function() {
    location.href = 'writePost';
});
$('#deletePostBtn').click(function() {
    const flag = confirm("本当にこのポストを削除してもよろしいですか?");
    if(flag) {
        const postId = $('#postId').val();
        location.href = `deletePost?postId=${postId}`;
    }
});
$('#updatePostBtn').click(function() {
    const postId = $('#postId').val();
    location.href = `updatePost?postId=${postId}`;
});