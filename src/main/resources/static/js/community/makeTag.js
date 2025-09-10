// 태그를 저장할 배열
let tags = [];

// 엔터 키 이벤트 처리
$('#tagInput').on('keydown', function(event) {
    if (event.key === 'Enter') {
        event.preventDefault();
        
        const inputValue = $(this).val().trim();
        
        if (inputValue !== '' && !tags.includes(inputValue)) {
            addTag(inputValue);
            $(this).val(''); // 입력 필드 초기화
        }
    }
});

// 태그 추가 함수
function addTag(tagText) {
    tags.push(tagText);
    
    // DOM에 태그 div 생성
    const $tagDiv = $('<div>').addClass('tag').html(`
        ${tagText}
        <button class="tagRemove" data-tag="${tagText}">×</button>
    `);
    
    $('#tagContainer').append($tagDiv);
    updateHiddenInput();
    
    console.log('현재 태그들:', tags);
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
    
    console.log('제거 후 태그들:', tags);
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