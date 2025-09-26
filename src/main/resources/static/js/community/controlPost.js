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

// --- 메시지 보내기 팝업 기능 ---
document.addEventListener('DOMContentLoaded', () => {
    const postContainer = document.querySelector('.post-wrapper');

    // 리뷰 목록 영역에 이벤트 리스너를 설정 (이벤트 위임)
    postContainer.addEventListener('click', (e) => {
        const authorArea = e.target.closest('.author-info')

        // 만약 작성자 영역이 클릭되었다면
        if (authorArea) {
            e.stopPropagation(); // 이벤트 버블링 방지

            const container = authorArea.closest('.author-container');
            const popup = container.querySelector('.send-message-popup');

            // 이미 열려있는 다른 팝업이 있다면 닫기
            document.querySelectorAll('.send-message-popup.active').forEach(activePopup => {
                if (activePopup !== popup) {
                    activePopup.classList.remove('active');
                }
            });

            // 현재 클릭한 팝업의 상태를 토글 (열고/닫기)
            popup.classList.toggle('active');
        }
    });

    // 문서 전체를 클릭했을 때 모든 팝업 닫기
    document.addEventListener('click', (e) => {
        // 만약 클릭한 곳이 팝업 버튼 자신이 아니라면
        if (!e.target.closest('.author-container')) {
            document.querySelectorAll('.send-message-popup.active').forEach(popup => {
                popup.classList.remove('active');
            });
        }
    });
});