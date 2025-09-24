// 게시글 작성 취소 버튼 이벤트 리스너
$('#cancelBtn').click(function() {
    const boardName = $('#boardName').val();
    // alert(boardName);
    location.href = `board?name=${boardName}`
});


// 게시글 작성 버튼 이벤트 리스너
$('#submitPostBtn').click(function() {
    writePost();
});

// 게시글 비동기 제출
function writePost() {

    // 유효성 검사
    const formData = validateForm();
    // 유효성 검사 통과 못 했을 시 종료
    if(!formData) return;

    // 비동기 요청으로 게시글 데이터 전달
    $.ajax({
        url: 'write',
        method: 'POST',
        data: formData,
        processData: false,
        contentType: false,
        // 작성 성공 시 해당 게시글 읽기
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
