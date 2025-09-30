const openModalBtn = document.getElementById('lectureRoom3');
const modal = document.getElementById('seatModal');
const closeModalBtn = modal.querySelector('.close-button');
const modalOverlay = modal.closest('.modal-overlay');
const reservationModal = document.getElementById('reservationModal');

// -------- 함수 정의 -------------
/**
 * 스터디룸 예약 모달 열기.
*/
async function openReservationModal(roomId, roomName) {
    // 필요한 부모 HTML 요소들을 선택합니다.
    const reservationModal = document.getElementById('reservationModal');
    const modalBody = reservationModal.querySelector('.modal-body');
    const modalTitle = document.getElementById('reservationModalTitle');

    // 모달 요소가 존재하는지 안전하게 확인합니다.
    if (!reservationModal || !modalBody) {
        console.error('모달 요소를 찾을 수 없습니다.');
        return;
    }

    modalTitle.textContent = `スタディルーム${roomId}`;

    try {
        // 서버에 해당 roomId의 예약 현황 HTML 조각을 요청합니다.
        const response = await fetch(`/scitHub/api/reservations/modal/${roomId}`);

        // 서버 응답이 성공적이지 않으면 오류를 발생시켜 catch 블록으로 넘깁니다.
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        // 성공적으로 받은 HTML 텍스트를 변수에 저장합니다.
        const modalContentHtml = await response.text();
        
        // 받아온 HTML을 모달의 내용 부분에 삽입합니다.
        modalBody.innerHTML = modalContentHtml;

        //-----
        const adminControlsContainer = document.getElementById('adminControlsContainer');
        const sourceAdminControls = modalBody.querySelector('.admin-controls'); // 불러온 내용 속 버튼 찾기

        if (sourceAdminControls) { // 관리자 버튼이 존재하면
            // 불러온 버튼을 컨테이너로 옮깁니다.
            adminControlsContainer.innerHTML = sourceAdminControls.innerHTML;
            // 원래 있던 자리의 버튼은 삭제합니다.
            sourceAdminControls.remove();
            // 컨테이너를 화면에 표시합니다.
            adminControlsContainer.style.display = 'block';
        } else {
            // 관리자가 아니라면 컨테이너를 숨깁니다.
            adminControlsContainer.style.display = 'none';
        }

        // 내용이 채워진 모달을 화면에 보여줍니다.
        reservationModal.classList.add('show');

    } catch (error) {
        // fetch 요청이나 서버 응답 처리 중 발생한 오류를 콘솔에 기록합니다.
        console.error('모달 데이터를 불러오는 데 실패했습니다.: ', error);
        // 사용자에게 오류가 발생했음을 알립니다.
        alert('予約情報の取得に失敗しました。しばらくしてから再度お試しください。');
    }
}


/**
 * 스터디룸 예약 저장 함수.
 * @param {HTMLElement} clickedButton 사용자가 클릭한 버튼 요소
 * @param {Integer} roomId 예약할 방의 Id
 * @param {Integer} timeSlotId 예약할 시간대의 Id
*/
async function makeReservation(clickedButton, roomId, timeSlotId) {
    // 서버로 보낼 데이터를 객체로 만듭니다.
    const reservationData = {
        classroomId: roomId,
        timeSlotId: timeSlotId
    };

    try {
        // fetch API를 사용해 서버에 POST 요청을 보냅니다.
        const response = await fetch('/scitHub/api/reservations', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(reservationData)
        });

        if (response.ok) {  // HTTP 상태 코드가 200~299 사이일 경우 (성공)
            // 서버가 보내준 생성된 예약 정보를 JSON 객체로 변환
            const newReservation = await response.json();
            
            // 클릭된 버튼을 기준으로 부모 time-slot-item 요소를 찾습니다.
            const timeSlotItem = clickedButton.closest('.time-slot-item');

            if (timeSlotItem) {
                // 이름(span)을 찾아 현재 로그인한 사용자 이름으로 변경합니다.
                // 예약자가 들어갈 span 지정
                const reserverNameSpan = timeSlotItem.querySelector('.reserver-name');
                reserverNameSpan.textContent = newReservation.username;

                // '예약취소' 버튼을 새로 생성하고 필요한 속성을 설정
                const newCancelButton = document.createElement('button');
                newCancelButton.className = 'reservation-btn cancelable';
                newCancelButton.textContent = '取り消し';

                // '예약취소' 기능을 위해, 새로 만든 버튼에 예약 ID를 심어줌.
                newCancelButton.dataset.reservationId = newReservation.reservationId;

                // '예약하기' 버튼(clickedButton)을 새로 만든 '예약취소' 버튼으로 교체
                timeSlotItem.replaceChild(newCancelButton, clickedButton);
            }
            alert('予約が完了しました！');
        } else {
            // 서버가 보낸 에러 메시지를 JSON 형태로 읽어옵니다.
            const errorData = await response.json();
            alert(`予約に失敗しました。${errorData.message}`);
        }
    } catch (error) {
        console.error('예약 처리 중 오류 발생:', error);
        alert('エラーが発生し、予約に失敗しました。');
    }
}

/**
 * 예약을 취소하는 함수
 * @param {HTMLElement} clickedButton 사용자가 클릭한 버튼 요소
 * @param {Integer} reservationId 취소할 예약의 Id
*/
async function cancelReservation(clickedButton, reservationId) {
    try {
        const response = await fetch(`/scitHub/api/reservations/${reservationId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            alert('予約がキャンセルされました。');

            // -- UI 업데이트 로직 --
            const timeSlotItem = clickedButton.closest('.time-slot-item');;
            const reserverNameSpan = timeSlotItem.querySelector('.reserver-name');

            // 이름 비우기
            reserverNameSpan.textContent = '-';

            
            // 관리자 여부 확인
            const adminControlsContainer = document.getElementById('adminControlsContainer');
            const isAdmin = adminControlsContainer && adminControlsContainer.style.display !== 'none';

            // 이제 이 조건문이 현재 사용자의 상태를 정확하게 반영합니다.
            if (isAdmin) {
                // 관리자용 : '예약 없음' 버튼 생성
                const newButton = document.createElement('button');
                newButton.className = 'reservation-btn reserved';
                newButton.disabled = true;
                newButton.textContent = '予約なし';
                timeSlotItem.replaceChild(newButton, clickedButton);
            } else {
                // 일반 사용자용 : '예약하기' 버튼 생성
                const newReserveButton = document.createElement('button');
                newReserveButton.className = 'reservation-btn available';
                newReserveButton.textContent = '予約する'; // '예약하기'의 일본어
                timeSlotItem.replaceChild(newReserveButton, clickedButton);
            }
        } else {
            alert('予約のキャンセルに失敗しました。');
        }
    } catch (error) {
        console.error('예약 취소 처리 중 오류 발생:', error);
        alert('エラーが発生し、予約のキャンセルに失敗しました。');
    }
}

/**
 * 강의실을 활성화 비활성화 하는 함수
*/
async function toggleRoomStatus(buttonElement) {
    // 전달받은 버튼 요소의 dataset에서 roomId와 roomName을 추출합니다.
    const roomId = buttonElement.dataset.roomId;
    const roomName = buttonElement.dataset.roomName;
    
    if(!confirm('本当にこの教室の状態を変更しますか。')) {
        return;
    }

    try {
        const response = await fetch(`/scitHub/api/classrooms/${roomId}/toggle-status`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            const result = await response.json();
            alert(result.message);

            // 토글한 교실 요소 찾기
            const roomElement = document.querySelector(`[data-room-id="${roomId}"]`);
            
            if (roomElement) {
                if (result.isActive) {
                    // 활성화 - 클래스 추가
                    roomElement.classList.add('active-room');
                } else {
                    // 비활성화 - 클래스 제거
                    roomElement.classList.remove('active-room');
                }
            }

            // 모달 새로고침
            openReservationModal(roomId,roomName);
        } else {
            // 서버가 응답으로 보낸 텍스트 에러 메시지를 읽어옵니다.
            const errorMessage = await response.text(); 
            // 읽어온 메시지를 alert 창에 보여줍니다.
            alert('状態の変更に失敗しました。');
        }
    } catch (error) {
        console.error('Error', error);
        alert('エラーが発生しました。');
    }
}
//페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', function() {
    // 스터디룸 열개에 모달 여는 이벤트를 넣기 위한 코드 (이벤트 위임)
    // 스터디룸 전체를 감싸는 부모 요소를 선택
    const seatmap = document.querySelector('.seatmap');
    console.log('seatmap:', seatmap);

    // ------------ 이벤트 리스너 -----------------
    // 부모 요소에 클릭 이벤트 리스너 등록
    seatmap.addEventListener('click', function(event) {
        // 클릭된 요소에서 가장 가까운 .room 클래스를 가진 부모 요소를 찾는 코드.
        const clickedRoom = event.target.closest('a.room');

        console.log('clickedRoom:', clickedRoom);
        if (clickedRoom) {
            event.preventDefault(); // a 태그의 기본 동작(페이지 이동) 방지

            // 클릭된 강의실 Id을 data-room-id 속성에서 가져옴
            const roomId = clickedRoom.dataset.roomId;
            const roomName = clickedRoom.dataset.roomName;
            console.log(`강의실 ${roomId}번이 클릭되었습니다.`);
            console.log(`강의실 ${roomName}번이 클릭되었습니다.`)

            // 가져온 roomId로 모달 여는 함수 호출
            openReservationModal(roomId, roomName);
        }
    });

    // 모달 내부 클릭 이벤트 처리 (이벤트 위임)
    const reservationModal = document.getElementById('reservationModal');

    reservationModal.addEventListener('click', function(event){
        // 클릭된 요소
        const clickedButton = event.target;

        // 클릭된 요소가 예약버튼이고, disabled 상태가 아닐 때만 코드를 실행합니다.
        if (clickedButton.matches('.reservation-btn') && !clickedButton.disabled) {
            // 'available' 클래스가 있을 경우: 예약 기능 실행
            if (clickedButton.classList.contains('available')) {
                console.log('예약하기 버튼이 클릭되었습니다.');

                // 클릭된 버튼에서 가장 가까운 부모 <ul>을 찾아 roomId를 가져옵니다.
                const parentList = clickedButton.closest('.time-slots-list');
                const roomId = parentList.dataset.roomId;

                // 클릭된 버튼에서 가장 가까운 부모 <li>를 찾아 timeSlotId를 가져옵니다.
                const parentItem = clickedButton.closest('.time-slot-item');
                const timeSlotId = parentItem.dataset.timeslotId;

                // --- 디버깅을 위한 코드 추가 ---
                console.log('--- 예약 요청 전 ID 값 확인 ---');
                console.log('클릭된 버튼:', clickedButton);
                console.log('찾은 부모 li:', parentItem);
                console.log('찾은 부모 ul:', parentList);
                console.log('추출된 roomId:', roomId);
                console.log('추출된 timeSlotId:', timeSlotId);

                // 예약 저장하는 함수 호출
                makeReservation(clickedButton, roomId, timeSlotId);
            }

            // 'cancelable' 클래스가 있을 경우: 예약 취소 기능 실행
            else if (clickedButton.classList.contains('cancelable')) {
                console.log('예약취소 버튼이 클릭되었습니다.');
                
                const reservationId = clickedButton.dataset.reservationId;
                if (confirm('本当に予約をキャンセルしますか?')) {
                    cancelReservation(clickedButton, reservationId);
                }
            }
        }
    })
});

// '講義室3' 클릭 시 모달 열기
openModalBtn.addEventListener('click', (e) => {
    e.preventDefault();
    modal.style.display = 'flex';
});

// 닫기 버튼 클릭 시 모달 닫기
closeModalBtn.addEventListener('click', () => {
    modal.style.display = 'none';
});

// 모달 바깥 영역 클릭 시 모달 닫기
modalOverlay.addEventListener('click', (e) => {
    if (e.target === modalOverlay) {
        modal.style.display = 'none';
    }
});

// 모달 바깥 영역 클릭 시 모달 닫기
reservationModal.addEventListener('click', (e) => {
    if (e.target === reservationModal) {
        reservationModal.classList.remove('show');
    }
});