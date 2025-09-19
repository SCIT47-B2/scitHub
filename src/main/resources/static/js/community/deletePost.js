// 비동기 요청으로 게시글 삭제 요청
function deletePost() {

    // 게시글 식별자 저장
    const postId = $('#postId').val();

    // 비동기 요청으로 게시글 데이터 전달
    $.ajax({
        url: 'deletePost?postId=' + postId,
        method: 'delete',
        data: formData,
        processData: false,
        contentType: false,
        // 수정 성공 시 해당 게시글 읽기
        success: function() {
            location.href = "home";
        },
        // 실패 시 게시판 홈으로 이동
        error: function(e) {
            alert(e.statusText);
            location.href = "home";
        }
    });
}