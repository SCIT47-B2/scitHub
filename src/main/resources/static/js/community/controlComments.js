/**
 *  댓글 목록 가져오기(ajax)
 */
function loadComments() {
    let data = {
        postId: $('#postId').val()
    };

    $.ajax({
        url: 'commentList', // 기존에 사용하시던 URL
        type: 'get',
        data: data,
        dataType: 'json',
        success: function(commentList) { // success 콜백의 파라미터 이름을 commentList로 사용
            const commentListContainer = $('.comment-list');
            commentListContainer.empty();

            if (!commentList || commentList.length === 0) {
                commentListContainer.html('<div class="no-comments">コメントがありません。</div>');
                return;
            }

            // $(commentList).each를 사용하여 기존 코드 형식 유지
            $(commentList).each(function(index, comment) {
                const commentHtml = createCommentHTML(comment);
                commentListContainer.append(commentHtml);
            });

            // 댓글 개수 출력
            const commentCount = commentList.length;
            console.log(commentCount);
            $('.commentCountDisplay').text(commentCount)
        },
        error: function() {
            alert('コメントの読み込みに失敗しました。');
        }
    });
}

/**
 * 댓글 데이터 객체(CommentDTO)를 받아 HTML 문자열을 생성하는 헬퍼 함수
 * @param {object} comment - 서버에서 받은 CommentDTO 객체
 */
function createCommentHTML(comment) {
    // DTO의 createdAt 필드를 사용합니다.
    const formattedDate = formatDate(comment.createdAt);

    // DTO의 canEdit 필드를 사용하여 수정/삭제 버튼 표시를 결정합니다.
    const actionButtons = comment.canEdit ? `
        <div class="comment-actions">
            <button class="comment-action-btn edit-btn" data-comment-id="${comment.commentId}"><i class="fa-solid fa-pen-to-square"></i></button>
            <button class="comment-action-btn delete-btn" data-comment-id="${comment.commentId}"><i class="fa-solid fa-trash"></i></button>
        </div>
    ` : '';

    const avatarUrl = comment.avatarUrl ? `/scitHub/images/avatar/${comment.avatarUrl}` : "/images/chiikawaPuzzle.png";
    // DTO의 필드명을 사용하여 HTML을 구성합니다.
    return `
        <div class="comment" data-comment-id="${comment.commentId}">
            <img src="${avatarUrl}" alt="프로필 사진" class="profile-pic-small">
            <div class="comment-content">
                <div class="comment-header">
                    <span class="comment-author">${comment.userNameKor}</span>
                    <span class="comment-date">${formattedDate}</span>
                </div>
                <p class="comment-text">${comment.content.replace(/\n/g, '<br>')}</p>
            </div>
            ${actionButtons}
        </div>
    `;
}

/**
 * 날짜/시간 데이터를 'yyyy.MM.dd HH:mm' 형식의 문자열로 변환하는 헬퍼 함수
 * @param {string | Date} dateString
 */
function formatDate(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}年 ${month}月 ${day}日 ${hours}:${minutes}`;
}


// --- 이벤트 핸들러 (기존 형식 반영) ---

/**
 * 새 댓글 작성 버튼 클릭 시 호출되는 함수
 */
function inputButtonClick() {
    const postId = $('#postId').val();
    const content = $('#commentBox').val().trim(); // 새 댓글 입력창 ID를 'commentBox'으로 가정

    if (content === '') {
        alert('コメントを入力して下さい。');
        return;
    }

    $.ajax({
        url: 'writeComment', // 기존에 사용하시던 URL
        type: 'post',
        data: {
            postId: postId,
            content: content
        },
        success: function() {
            $('#commentBox').val(''); // 입력창 비우기
            loadComments(); // 댓글 목록 새로고침
        },
        error: function() {
            alert('コメント送信に失敗しました。');
        }
    });
}

// 새 댓글 작성 버튼에 클릭 이벤트 연결
// HTML에 <button onclick="inputButtonClick()">댓글 남기기</button> 와 같이 연결하거나 아래 코드를 사용
$('#submit-comment-btn').on('click', inputButtonClick); // 버튼 ID를 'submit-comment-btn'으로 가정

/**
 * 동적으로 생성된 댓글의 삭제 버튼 클릭 처리 (이벤트 위임)
 */
$('.comment-list').on('click', '.delete-btn', function() {
    const commentId = $(this).data('comment-id');
    
    if (confirm('本当にこのコメントを削除してよろしいですか?')) {
        $.ajax({
            url: `deleteComment/${commentId}`, // 삭제 처리를 위한 URL
            type: 'delete',
            success: function() {
                alert('コメントを削除しました。');
                // 특정 댓글 엘리먼트만 삭제하여 성능 향상
                $(`.comment[data-comment-id=${commentId}]`).remove();
                // 또는 전체 목록 새로고침
                // loadComments();
                // 댓글 개수 변경
                const commentCount = $('.comment-list').children().length;
                console.log(commentCount);
                $('.commentCountDisplay').text(commentCount);
            },
            error: function() {
                alert('コメントの削除に失敗しました。');
            }
        });
    }
});

/**
 * 동적으로 생성된 댓글의 수정 버튼 클릭 처리
 */
$('.comment-list').on('click', '.edit-btn', function() {
    // 이미 다른 댓글이 수정 모드일 경우, 원래 상태로 되돌립니다.
    if ($('.comment-edit-mode').length > 0) {
        const editingCommentId = $('.comment-edit-mode').data('comment-id');
        cancelEdit(editingCommentId);
    }

    const commentId = $(this).data('comment-id');
    const commentDiv = $(`.comment[data-comment-id=${commentId}]`);
    const commentTextP = commentDiv.find('.comment-text');
    
    // <br> 태그를 다시 줄바꿈(\n) 문자로 변환합니다.
    const originalContent = commentTextP.html().replace(/<br\s*\/?>/gi, '\n');

    // 수정용 textarea와 버튼들을 포함하는 HTML 생성
    const editFormHtml = `
        <div class="comment-edit-form">
            <textarea class="comment-edit-textarea">${originalContent}</textarea>
            <div class="comment-edit-actions">
                <button class="edit-action-btn cancel-edit-btn" data-comment-id="${commentId}" title="取り消し">
                    <i class="fa-solid fa-xmark"></i>
                </button>
                <button class="edit-action-btn save-edit-btn" data-comment-id="${commentId}" title="修正完了">
                    <i class="fa-solid fa-check"></i>
                </button>
            </div>
        </div>
    `;

    // 기존 댓글 내용을 숨기고 수정 폼을 보여줍니다.
    commentTextP.hide();
    commentDiv.find('.comment-content').append(editFormHtml);

    // 수정 모드임을 표시하기 위해 클래스 추가
    commentDiv.addClass('comment-edit-mode');
});

/**
 * '수정 완료' 버튼 클릭 처리 (이벤트 위임)
 */
$('.comment-list').on('click', '.save-edit-btn', function() {
    const commentId = $(this).data('comment-id');
    const commentDiv = $(`.comment[data-comment-id=${commentId}]`);
    const newContent = commentDiv.find('.comment-edit-textarea').val().trim();

    if (newContent === '') {
        alert('コメントの修正内容を入力して下さい。');
        return;
    }

    $.ajax({
        url: 'updateComment', // 수정 처리를 위한 URL
        type: 'patch',
        contentType: 'application/json',
        data: JSON.stringify({
            commentId: commentId,
            content: newContent
        }),
        success: function() {
            // 수정된 내용을 p 태그에 반영하고 수정 폼을 제거합니다.
            const commentTextP = commentDiv.find('.comment-text');
            commentTextP.html(newContent.replace(/\n/g, '<br>')).show();
            commentDiv.find('.comment-edit-form').remove();
            commentDiv.removeClass('comment-edit-mode');
            // alert('コメントが修正されました。');
        },
        error: function() {
            alert('コメントの修正に失敗しました。');
            // 실패 시 원래 상태로 복구
            cancelEdit(commentId);
        }
    });
});

/**
 * '취소' 버튼 클릭 처리 (이벤트 위임) 및 취소 로직 함수화
 */
$('.comment-list').on('click', '.cancel-edit-btn', function() {
    const commentId = $(this).data('comment-id');
    cancelEdit(commentId);
});

/**
 * 댓글 수정을 취소하고 원래 상태로 되돌리는 함수
 * @param {number} commentId
 */
function cancelEdit(commentId) {
    const commentDiv = $(`.comment[data-comment-id=${commentId}]`);
    if (commentDiv.length > 0) {
        commentDiv.find('.comment-text').show();
        commentDiv.find('.comment-edit-form').remove();
        commentDiv.removeClass('comment-edit-mode');
    }
}

/**
 * 동적으로 생성된 textarea의 높이를 내용에 맞게 자동 조절 (이벤트 위임)
 */
$('.comment-list').on('input', '.comment-edit-textarea', function() {
    $(this).css('height', 'auto'); // 높이를 초기화
    $(this).css('height', this.scrollHeight + 'px'); // 스크롤 높이에 맞춰 높이 설정
});