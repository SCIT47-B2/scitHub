 // 게시판 폼에 입력한 데이터를 서버 사이드에 비동기 제출
function validateForm() {
    //유효성 검사
    const board = $('#boardDropdown').val();
    const title = $('#title').val();
    const content = editor.getData();
    const tagList = $('#tagsData').val();

    // 각 부분에 내용이 없을 시 알림 문구
    if (!board) {
        alert('ポストを作成する掲示板を選択して下さい。');
        return;
    }
    if (!title) {
        alert('タイトルを入力して下さい。');
        return;
    }
    if (!content) {
        alert('ポストの内容を入力して下さい。');
        return;
    }

    // 게시글 관련 데이터들을 form 제출과 같은 형식으로 가공
    const formData = new FormData();
    formData.append('board', board);
    formData.append('title', title);
    formData.append('content', content);
    formData.append('tagList', tagList);

    return formData;
}