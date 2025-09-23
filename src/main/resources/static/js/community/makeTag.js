// 태그를 저장할 배열
let tags = [];

// 에러 메시지를 출력할 div
const $tagError = $('#tagError');

// 엔터 키 이벤트 처리
$('#tagInput').on('keydown', function(event) {
    if (event.key === 'Enter') {
        event.preventDefault();
        
        const inputValue = $(this).val().trim();
        
        // 태그 입력이 정상적으로 종료되면
        if(addTag(inputValue)) {
            $(this).val(''); // 입력 필드 초기화
        };
    }
});

// 사용자가 입력을 시작하면 에러 메시지 숨김
$('#tagInput').on('input', function() {
    if ($tagError.is(':visible')) {
        $tagError.hide();
    }
});

// 태그 추가 함수
function addTag(tagText) {

    // 태그 유효성 검사 실패 시 태그를 더하지 않음
    if (!tagValidation(tagText)) {
        $tagError.text('#で始まる2~20字のの文字、数字、_を空白無しで入力して下さい。');
        $tagError.show();
        return false;
    }

    $tagError.hide();

    if (tags.includes(tagText)) {
        $tagError.text('既に付いているタグです。');
        $tagError.show();
        return false;
    }

    // 유효성 검사 통과 시 태그 배열에 태그 추가
    tags.push(tagText);
    
    // DOM에 태그 div 생성
    const $tagDiv = $('<div>').addClass('tag').html(`
        ${tagText}
        <button class="tagRemove" data-tag="${tagText}">×</button>
    `);
    
    $('#tagContainer').append($tagDiv);
    updateHiddenInput();
    
    console.log('現在付いているタグ:', tags);
    return true;
}

function tagValidation(tagText) {
    const regex = /^#[\p{L}\p{N}_]{1,19}$/u;
    return regex.test(tagText);

}

// 태그 제거 이벤트 (동적 이벤트 바인딩)
$(document).on('click', '.tagRemove', function() {
    const tagText = $(this).data('tag');
    removeTag(tagText, $(this).parent());
});

// 태그 제거 함수
function removeTag(tagText, $tagElement) {
    const index = tags.indexOf(tagText);
    if (index > -1) {
        tags.splice(index, 1);
    }
    
    $tagElement.remove();
    updateHiddenInput();
    
    console.log('除去後付いているタグ:', tags);
}

// Hidden input 업데이트
function updateHiddenInput() {
    $('#tagsData').val(tags);
}

// 전역 함수들
window.addTag = addTag;
window.removeTag = removeTag;
window.getAllTags = function() { return tags; };
window.clearAllTags = function() {
    tags = [];
    $('#tagContainer').empty();
    updateHiddenInput();
};