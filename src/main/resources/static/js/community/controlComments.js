// 댓글 저장
function inputButtonClick() {
    // 작성자명 불러오기 : 현재 로그인 중인 유저의 이름을 불러와야 함
    let postId   = $('#postId').val();
    let userName = $('#currentUser').val();
    let content  = $('#commentBox').val().trim();
    
    if (content == '') {
        alert('댓글 내용을 입력하세요.');
        return;
    }
    
    $.ajax({
        url: 'writeComment',
        type: 'post',
        data: {
            postId: postId,
            userName: userName,
            content: content},
        success: function() {
            $('#comment').val('');
            // 댓글 목록 재로딩
            commentList();
        },
        error: function() {
            alert('댓글을 저장하지 못했습니다.');
        }
    });
}

// 댓글 목록 불러오기
function commentList() {
    let data = { postId: $('#postId').val() };
    $.ajax({
        url: 'commentList',
        type: 'get',
        data: data,
        dataType: 'json',
        success: function(commentList) {
            let str = ``;
            let commentCount = 0;
            
            if (!commentList || commentList.length == 0) {

            } else {
                $(commentList).each(function(i, ob) {
                    str += `
                        <tr>
                            <td>${ob.username}</td>
                            <td class="commentContent" id="content${ob.commentId}">${ob.content}</td>
                            `;
                    if(ob.canEdit) {
                        str += `
                            <td>
                                <button class="deleteButton"
                                        data-commentid="${ob.commentId}">삭제</button>
                            </td>
                            <td>
                                <button class="updateButton"
                                        data-commentid="${ob.commentId}"
                                        data-content="${ob.content}">수정</button>
                            </td>
                        </tr>
                    `;
                    }
                    commentCount++;
                });
            }
            
            $('#commentTbody').html(str);
            $('.commentCountDisplay').text(`💬 ${commentCount}`);
            
            //이벤트 등록
        },
        error: function() {
            alert('댓글 목록 조회 실패');
        }
    });
}

// 삭제
function deleteFunc() {
    let result = confirm("정말로 이 댓글을 삭제하시겠습니까?");
    
    if (result) {
        // 정말 이상하게도 id -> Id로 표기 시 에러 남
        let commentId = $(this).data('commentid');
        $.ajax({
            url: `deleteComment/${commentId}`,
            type: 'delete',	 //HTTP 메서드 방식(삭제)
            success: function() {
                commentList();
            },
            error: function() {
                alert('삭제 실패');
            }
        });
    }
}

// 수정
function inputConfirmFunc(updateCommentButtonId, commentBoxId) {
    return new Promise((resolve) => {
        const updateButton = document.getElementById(updateCommentButtonId);
        
        // 취소 버튼 생성
        const cancelButton = document.createElement('button');
        cancelButton.textContent = '취소';
        cancelButton.type = 'button';
        updateButton.parentNode.appendChild(cancelButton);
        
        function handleUpdate() {
            const updateText = document.getElementById(commentBoxId).value;
            cleanup();
            resolve({ action: 'update', text: updateText });
        }
        
        function handleCancel() {
            cleanup();
            resolve({ action: 'cancel', text: null });
        }
        
        function cleanup() {
            updateButton.removeEventListener('click', handleUpdate);
            cancelButton.removeEventListener('click', handleCancel);
            cancelButton.remove();
        }
        
        updateButton.addEventListener('click', handleUpdate);
        cancelButton.addEventListener('click', handleCancel);
    });
}

async function updateFunc() {
    const commentId = $(this).data('commentid');
    const commentData = $(this).data('content');
    
    const commentAreaId = '#content' + commentId;
    const originalContent = $(commentAreaId).html(); // 원래 내용 저장
    
    const commentBoxId = 'commentBox' + commentId;
    const updateCommentButtonId = 'updateComment' + commentId + 'Button';
    
    // input 폼으로 변경
    const commentAreaStr = `
        <input id="${commentBoxId}" value="${commentData}">
        <button type="button" id="${updateCommentButtonId}">수정완료</button>
    `;
    $(commentAreaId).html(commentAreaStr);
    
    try {
        const result = await inputConfirmFunc(updateCommentButtonId, commentBoxId);
        
        if (result.action === 'cancel') {
            // 취소 시 원래 내용으로 복원
            $(commentAreaId).html(originalContent);
            return;
        }
        
        // 수정 진행
        if (result.text && result.text.trim() !== '') {
            const updateData = {
                commentId: commentId,
                content: result.text
            };
            console.log(updateData);
            
            // 성공 시 새로운 내용으로 표시
            $(commentAreaId).html(result.text);

            // Ajax 요청
            $.ajax({
                url: 'updateComment',
                type: 'patch',
                data: JSON.stringify(updateData),
                contentType: 'application/json',
                success: function() {
                    commentList(); // 댓글 목록 새로고침
                },
                error: function() {
                    alert('수정 실패');
                    // 실패 시 원래 내용으로 복원
                    $(commentAreaId).html(originalContent);
                }
            });
        } else {
            alert('수정할 내용을 입력해주세요!');
            $(commentAreaId).html(originalContent);
        }
    } catch (error) {
        $(commentAreaId).html(originalContent);
    }
}