 // 게시판 폼에 입력한 데이터를 서버 사이드에 비동기 제출
function submitPost() {
    //유효성 검사
    const board = $('#boardDropdown').val();
    const title = $('#title').val();
    const content = editor.getData();
    const tagList = $('#tagsData').val();

    // 각 부분에 내용이 없을 시 알림 문구
    if (!board) {
        alert('게시판을 선택해주세요');
        return;
    }
    if (!title) {
        alert('제목을 입력해주세요');
        return;
    }
    if (!content) {
        alert('내용을 입력해주세요');
        return;
    }

    // 게시글 관련 데이터들을 form 제출과 같은 형식으로 가공
    const formData = new FormData();
    formData.append('board', board);
    formData.append('title', title);
    formData.append('content', content);
    formData.append('tagList', tagList);

    // 비동기 요청으로 게시글 데이터 전달
    $.ajax({
        url: 'write',
        method: 'POST',
        data: formData,
        processData: false,
        contentType: false
    });

    // 데이터 전달 후 이동할 페이지
    location.href = "home";
}
$('#submitPostBtn').click(function() {
    submitPost();
});