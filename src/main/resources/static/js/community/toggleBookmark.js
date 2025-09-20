$("#bookmarkBtn").click(bookmarkFunc);

function bookmarkFunc() {
    // 게시글 고유 식별자 받아오기
    const postId = $('#postId').val();
    // 북마크 요청 보내기(비동기)
    $.ajax({
        url: 'bookmarkPost?postId=' + postId,
        method: 'POST',
        // 요청 성공 시 북마크 표시
        success: function(data) {
            // 북마크 토글 처리 시 아이콘 변경
            // 사실 북마크 여부를 받아와서 처리해야 하지만, T/F이기 때문에 야매로 처리
            const originalLetter = $(".isBookmarkedDisplay").text();
            const newLetter = originalLetter == '💙' ? '🤍' : '💙';
            $(".isBookmarkedDisplay").text(newLetter);
        },
        // 실패 시 alert창 띄우기
        error: function(e) {
            alert(e.responseText);
        }
    });
}