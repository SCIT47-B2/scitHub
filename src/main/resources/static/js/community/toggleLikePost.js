$("#likeBtn").click(likeFunc);

function likeFunc() {
    // 게시글 고유 식별자 받아오기
    const postId = $('#postId').val();
    // 좋아요 요청 보내기(비동기)
    $.ajax({
        url: 'likePost?postId=' + postId,
        method: 'POST',
        // 요청 성공 시 좋아요 개수 갱신
        success: function(data) {
            $(".likeCountDisplay").text(data);
            // 좋아요 토글 처리 시 아이콘 변경 (클래스 추가/삭제로 아이콘 색 변경)
            if ($(".isLikedDisplay").hasClass('active')) {
                $(".isLikedDisplay").removeClass('active');
            } else {
                $(".isLikedDisplay").addClass('active');
            }
        },
        // 실패 시 alert창 띄우기
        error: function(e) {
            alert(e.responseText);
        }
    });
}