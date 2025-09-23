$('#updatePostBtn').click(function() {
    updatePost();
});

$('#cancelBtn').click(function() {
    // 게시글 식별자 저장
    const postId = $('#postId').val();
    location.href = `readPost?postId=${postId}`;
});

// 게시글 수정 요청(비동기)
function updatePost() {

    // 게시글 식별자 저장
    const postId = $('#postId').val();
    // 유효성 검사
    const formData = validateForm();
    // 유효성 검사 통과 못 했을 시 종료
    if(!formData) return;

    // 비동기 요청으로 게시글 데이터 전달
    $.ajax({
        url: 'updatePost?postId=' + postId,
        method: 'PATCH',
        data: formData,
        processData: false,
        contentType: false,
        // 수정 성공 시 해당 게시글 읽기
        success: function(data) {
            location.href = "readPost?postId=" + data;
        },
        // 실패 시 게시판 홈으로 이동
        error: function(e) {
            alert(e.statusText);
            location.href = "home";
        }
    });
}