function displayTags() {
    // hidden input에서 태그 데이터 가져오기
    const tagsData = $('#tagsData').val();

    console.log(tagsData);

    // 태그 데이터가 없거나 빈 문자열인 경우 처리
    if (!tagsData || tagsData.trim() === '' || tagsData === '[]') {
        return;
    }

    try {
        // 문자열을 배열로 파싱
        let tagsArray = parseTagsString(tagsData);

        // 태그 HTML 생성
        let tagsHtml = '';
        tagsArray.forEach(function(tag) {
            if (tag.trim() !== '') {
                tagsHtml += `<div class="tag" data-tag="${tag}">${tag} </div>`;
                console.log(tag);
            }
        });

        // 태그가 존재하면 컨테이너에 추가
        if (tagsHtml !== '') {
            $('#tagsContainer').html(tagsHtml);
        }

        // 태그 클릭 이벤트 바인딩
        bindTagClickEvents();

    } catch (error) {
        console.error('タグのパーシング中エラー発生:', error);
        $('#tagsContainer').html('<span class="no-tags">タグの読込みに失敗しました。</span>');
    }
}

// 태그 문자열을 배열로 파싱하는 함수
function parseTagsString(tagsString) {
    // '[#태그1, #태그2, #태그3]' 형식을 파싱

    // 1. 대괄호 제거
    let cleanString = tagsString.replace(/^\[|\]$/g, '');

    // 2. 빈 문자열 체크
    if (cleanString.trim() === '') {
        return [];
    }

    // 3. 쉼표로 분할
    let tagsArray = cleanString.split(',');

    // 4. 각 태그 정리 (앞뒤 공백 제거)
    tagsArray = tagsArray.map(function(tag) {
        return tag.trim();
    });

    // 5. 빈 태그 필터링
    tagsArray = tagsArray.filter(function(tag) {
        return tag !== '';
    });

    return tagsArray;
}

// 태그 클릭 이벤트 바인딩
function bindTagClickEvents() {
    $('.tag-item').on('click', function() {
        const tagName = $(this).data('tag');
        // 해당 태그로 검색하거나 필터링하는 기능
        searchByTag(tagName);
    });
}

// 태그로 검색하는 함수 (선택사항)
function searchByTag(tagName) {
    // 태그 검색 페이지로 이동하거나 검색 기능 실행
    const searchUrl = '/community/board?searchType=tag&keyword=' + encodeURIComponent(tagName);
    window.location.href = searchUrl;
}

// 대안: JSON.parse를 사용하는 방법 (서버에서 올바른 JSON 형식으로 전달되는 경우)
function parseTagsWithJSON(tagsString) {
    try {
        // 서버에서 ["#태그1", "#태그2", "#태그3"] 형식으로 전달되는 경우
        return JSON.parse(tagsString);
    } catch (error) {
        console.error('JSON パーシングエラー:', error);
        return parseTagsString(tagsString); // 기본 파싱 방법으로 대체
    }
}