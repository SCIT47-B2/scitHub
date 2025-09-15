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
            // 좋아요 토글 처리 시 아이콘 변경
            // 사실 좋아요 여부를 받아와서 처리해야 하지만, T/F이기 때문에 야매로 처리
            const originalLetter = $(".isLikedDisplay").text();
            const newLetter = originalLetter == '💙' ? '🤍' : '💙';
            $(".isLikedDisplay").text(newLetter);
        },
        // 실패 시 alert창 띄우기
        error: function(e) {
            alert(e.responseText);
        }
    });
}