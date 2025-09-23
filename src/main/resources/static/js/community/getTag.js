// 기존에 존재하는 태그 불러오기 함수
function displayTag() {

    // 서버에서 받아온 기존 태그 리스트 불러오기
    tags = JSON.parse($('#tagsData').val());
    
    // DOM에 태그 div 생성
    tags.forEach(tag => {
        const $tagDiv = $('<div>').addClass('tag').html(`
            ${tag}
            <button class="tagRemove" data-tag="${tag}">×</button>
        `);

        $('#tagContainer').append($tagDiv);
        updateHiddenInput();

        console.log('現在付いているタグ:', tags);
    });
}
// 초기 실행 시 기존 태그 불러오기
$(document).ready(displayTag());