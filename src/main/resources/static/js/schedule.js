// /** 스케줄 페이지 스크립트
//  * @desc   FullCalendar 초기화 및 모달(등록/상세/수정/삭제) 로직
//  * @param  window.currentUser {userId, username, role} - 서버에서 주입(Thymeleaf inline)
//  * @return 없음
//  */

/** 시작·종료 input의 min/max 동기화 리스너 추가 */
function addDateValidationListeners() {
  const startDateTimeInput = document.querySelector('#eventStartDate');
  const endDateTimeInput = document.querySelector('#eventEndDate');
  const startDateInput = document.querySelector('#startDate');
  const endDateInput = document.querySelector('#endDate');

  startDateTimeInput.addEventListener('change', () => {
    endDateTimeInput.min = startDateTimeInput.value || '';
  });
  endDateTimeInput.addEventListener('change', () => {
    startDateTimeInput.max = endDateTimeInput.value || '';
  });
  startDateInput.addEventListener('change', () => {
    endDateInput.min = startDateInput.value || '';
  });
  endDateInput.addEventListener('change', () => {
    startDateInput.max = endDateInput.value || '';
  });
}

/** 현재 값 기준으로 즉시 min/max 제약 갱신 */
function updateDateConstraints() {
  const startDT = document.querySelector('#eventStartDate');
  const endDT = document.querySelector('#eventEndDate');
  const startD = document.querySelector('#startDate');
  const endD = document.querySelector('#endDate');

  startDT.max = '';
  endDT.min = '';
  startD.max = '';
  endD.min = '';

  if (startDT.value) endDT.min = startDT.value;
  if (endDT.value) startDT.max = endDT.value;
  if (startD.value) endD.min = startD.value;
  if (endD.value) startD.max = endD.value;
}

/** 모달 닫기 */
function closeModal() {
  document.querySelector('#eventModal').classList.remove('show');
  window.currentEventInfo = null;
}

/** 폼 읽기/편집 토글 */
function setFormEditable(isEditable) {
  document.querySelector('#eventTitle').readOnly = !isEditable;
  document.querySelector('#eventContent').readOnly = !isEditable;
  document.querySelector('#eventStartDate').readOnly = !isEditable;
  document.querySelector('#eventEndDate').readOnly = !isEditable;
  document.querySelector('#startDate').readOnly = !isEditable;
  document.querySelector('#endDate').readOnly = !isEditable;
  document.querySelector('#isAllDay').disabled = !isEditable;
}

/** 종일 여부에 따라 날짜/시간 입력 토글 */
function toggleDateInputs() {
  const isAllDay = document.getElementById('isAllDay').checked;

  if (isAllDay) {
    document.querySelector('#dateTimeInputs').style.display = 'none';
    document.querySelector('#dateTimeEndInputs').style.display = 'none';
    document.querySelector('#dateOnlyInputs').style.display = 'block';
    document.querySelector('#dateOnlyEndInputs').style.display = 'block';

    const startDateTime = document.querySelector('#eventStartDate').value;
    const endDateTime = document.querySelector('#eventEndDate').value;

    if (startDateTime) document.querySelector('#startDate').value = startDateTime.split('T')[0];
    if (endDateTime) document.querySelector('#endDate').value = endDateTime.split('T')[0];
  } else {
    document.querySelector('#dateTimeInputs').style.display = 'block';
    document.querySelector('#dateTimeEndInputs').style.display = 'block';
    document.querySelector('#dateOnlyInputs').style.display = 'none';
    document.querySelector('#dateOnlyEndInputs').style.display = 'none';

    const startDate = document.querySelector('#startDate').value;
    const endDate = document.querySelector('#endDate').value;

    if (startDate) document.querySelector('#eventStartDate').value = startDate + 'T09:00';
    if (endDate) document.querySelector('#eventEndDate').value = endDate + 'T18:00';
  }
}

/** 상세 보기에서 수정 모드로 전환 */
function enableEditMode() {
  document.querySelector('#modalTitle').textContent = '日程修正';
  setFormEditable(true);
  document.querySelector('#modalButtons').innerHTML = `
    <button type="button" class="btn btn-secondary" onclick="disableEditMode()">キャンセル</button>
    <button type="submit" class="btn btn-primary" id="submitBtn">貯蔵</button>
  `;
}

/** 수정 모드 취소 → 상세 보기 복귀 */
function disableEditMode() {
  document.querySelector('#modalTitle').textContent = '日程詳細';
  setFormEditable(false);
  document.querySelector('#modalButtons').innerHTML = `
    <button type="button" class="btn btn-secondary" onclick="closeModal()">閉じる</button>
    <button type="button" class="btn btn-danger" id="deleteBtn">削除</button>
    <button type="button" class="btn btn-primary" onclick="enableEditMode()">修整</button>
  `;
  populateForm(window.currentEventInfo.event);
}

/** 상세 보기 폼 채우기 */
function populateForm(event) {
  document.getElementById('eventId').value = event.id;
  document.getElementById('eventTitle').value = event.title;
  document.getElementById('eventContent').value = event.extendedProps.content;
  document.getElementById('isAllDay').checked = event.allDay;

  toggleDateInputs();

  if (event.allDay) {
    document.getElementById('startDate').value = event.startStr;
    document.getElementById('endDate').value = event.endStr
      ? new Date(new Date(event.endStr).getTime() - 86400000).toISOString().split('T')[0]
      : event.startStr;
  } else {
    document.getElementById('eventStartDate').value = event.start ? event.start.toISOString().slice(0, 16) : '';
    document.getElementById('eventEndDate').value = event.end ? event.end.toISOString().slice(0, 16) : '';
  }

  updateDateConstraints();
}

/** 서버 전송용 날짜 포맷팅(YYYY-MM-DDTHH:mm:ss) */
function formatDateForServer(date) {
  if (!date) return null;
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
}

/** 필터(전체/개인) 변경 시 이벤트 재조회 */
function handleFilterChange(calendar) {
  if (calendar) calendar.refetchEvents();
}

/** 모달 열기 */
function openModal(mode, info) {
  const eventModal = document.querySelector('#eventModal');
  const modalTitle = document.querySelector('#modalTitle');
  const eventForm = document.querySelector('#eventForm');
  const modalButtons = document.querySelector('#modalButtons');

  eventModal.classList.add('show');
  eventForm.reset();
  toggleDateInputs();
  setFormEditable(true);

  switch (mode) {
    case 'create': {
      modalTitle.textContent = '日程登録';
      document.querySelector('#eventId').value = '';

      if (!info || !info.dateStr) {
        document.getElementById('eventStartDate').value = '';
        document.getElementById('eventEndDate').value = '';
        document.getElementById('startDate').value = '';
        document.getElementById('endDate').value = '';
      } else {
        let selectDateTime;
        if (info.allDay) {
          const now = new Date();
          const hours = String(now.getHours()).padStart(2, '0');
          const minutes = String(now.getMinutes()).padStart(2, '0');
          selectDateTime = `${info.dateStr}T${hours}:${minutes}`;
        } else {
          selectDateTime = info.dateStr.slice(0, 16);
        }
        document.getElementById('eventStartDate').value = selectDateTime;
      }

      updateDateConstraints();

      modalButtons.innerHTML = `
        <button type="button" class="btn btn-secondary" onclick="closeModal()">キャンセル</button>
        <button type="submit" class="btn btn-primary" id="submitBtn">登録</button>
      `;
      break;
    }

    case 'view': {
      window.currentEventInfo = info;
      modalTitle.textContent = '日程詳細';
      populateForm(info.event);
      setFormEditable(false);

      const eventOwnerId = info.event.extendedProps.userId;
      const eventVisibility = info.event.extendedProps.visibility;

      const isAdmin = window.currentUser && window.currentUser.role === 'ROLE_ADMIN';
      const isOwner = window.currentUser && window.currentUser.username === eventOwnerId;

      let buttonsHTML = '<button type="button" class="btn btn-secondary" onclick="closeModal()">閉じる</button>';
      if ((isAdmin && eventVisibility === 'PUBLIC') ||
          (!isAdmin && isOwner && eventVisibility === 'PRIVATE')) {
        buttonsHTML = `
          <button type="button" class="btn btn-secondary" onclick="closeModal()">閉じる</button>
          <button type="button" class="btn btn-danger" id="deleteBtn">削除</button>
          <button type="button" class="btn btn-primary" id="editBtn">修整</button>
        `;
      }
      modalButtons.innerHTML = buttonsHTML;
      break;
    }
  }
}

/** 메인 초기화 */
document.addEventListener('DOMContentLoaded', function onReady() {
  // 공용 참조
  const eventModal = document.querySelector('#eventModal');
  const modalButtons = document.querySelector('#modalButtons');

  addDateValidationListeners();

  const calendarEl = document.getElementById('calendar');
  const calendar = new FullCalendar.Calendar(calendarEl, {
    initialView: 'dayGridMonth',
    locale: 'ja',
    editable: true,
    headerToolbar: { left: '', center: '', right: '' },
    buttonText: { today: 'Today', month: 'M', week: 'W', day: 'D' },

    dayHeaderContent: function (arg) {
      const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
      return dayNames[arg.date.getDay()];
    },

    selectable: true,
    firstDay: 0,
    fixedWeekCount: false,

    datesSet: function (info) {
      document.getElementById('calendarTitle').textContent = info.view.title;
    },

    events: function (info, successCallback, failureCallback) {
      const showPublic = document.querySelector('#adminToggle').checked;
      const showPrivate = document.querySelector('#personalToggle').checked;

      fetch(`/scitHub/api/calendar/events?showPublic=${showPublic}&showPrivate=${showPrivate}`)
        .then((response) => {
          if (!response.ok) throw new Error('Network response was not ok');
          return response.json();
        })
        .then((data) => {
          const events = [];
          for (let i = 0; i < data.length; i++) {
            const event = data[i];

            let isEventEditable = true;
            const isAdmin = window.currentUser && window.currentUser.role === 'ROLE_ADMIN';
            const isPublicEvent = event.visibility === 'PUBLIC';
            if (!isAdmin && isPublicEvent) isEventEditable = false;

            events.push({
              id: event.eventId,
              title: event.title,
              start: event.start,
              end: event.end,
              allDay: event.allDay,
              extendedProps: {
                content: event.content,
                visibility: event.visibility,
                userId: event.userId
              },
              editable: isEventEditable,
              color: event.visibility === 'PUBLIC' ? '#ff9f89' : '#3788d8'
            });
          }
          successCallback(events);
        })
        .catch((error) => {
          console.error('Fetch error:', error);
          failureCallback(error);
          alert('スケジュールの読み込みに失敗しました.');
        });
    },

    dateClick: function (info) {
      openModal('create', info);
    },

    eventClick: function (info) {
      openModal('view', info);
    },

    eventDrop: function (info) {
      if (!confirm('この変更を保存しますか?')) {
        info.revert();
        return;
      }
      const eventId = info.event.id;
      const newStart = formatDateForServer(info.event.start);
      const newEnd = info.event.end ? formatDateForServer(info.event.end) : null;
      const isAllDay = info.event.allDay;

      const eventUpdateData = {
        start: newStart,
        end: newEnd,
        allDay: isAllDay
      };

      fetch(`/scitHub/api/calendar/events/${eventId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(eventUpdateData)
      })
        .then((response) => {
          if (!response.ok) throw new Error('スケジュールの更新に失敗しました.');
          return response.json();
        })
        .then(() => {
          alert('日程が正常に変更されました.');
        })
        .catch((error) => {
          console.error('업데이트 실패:', error);
          alert('スケジュールの変更に失敗し、元の場所に戻します.');
          info.revert();
        });
    }
  });

  calendar.render();

  // 필터 체크박스
  const adminToggle = document.querySelector('#adminToggle');
  const personalToggle = document.querySelector('#personalToggle');
  adminToggle.addEventListener('change', () => handleFilterChange(calendar));
  personalToggle.addEventListener('change', () => handleFilterChange(calendar));

  // 모달 바깥 클릭 닫기
  eventModal.addEventListener('click', function (event) {
    if (event.target === eventModal) closeModal();
  });

  // 헤더 네비 버튼
  document.querySelector('#prevBtn').addEventListener('click', () => calendar.prev());
  document.querySelector('#nextBtn').addEventListener('click', () => calendar.next());
  document.querySelector('#todayBtn').addEventListener('click', () => calendar.today());

  // 뷰 전환 버튼
  document.querySelectorAll('.view-btn').forEach(function (btn) {
    btn.addEventListener('click', function () {
      document.querySelector('.view-btn.active').classList.remove('active');
      this.classList.add('active');
      calendar.changeView(this.dataset.view);
    });
  });

  // 일정 등록 버튼
  document.querySelector('#eventAddBtn').addEventListener('click', function () {
    openModal('create');
  });

  // 모달 내부 버튼(수정/삭제) 위임
  modalButtons.addEventListener('click', function (event) {
    const clicked = event.target;

    if (clicked.id === 'editBtn') {
      enableEditMode();
    }

    if (clicked.id === 'deleteBtn') {
      const isConfirmed = confirm('本当にこのスケジュールを削除しますか?');
      if (!isConfirmed) return;

      const eventId = window.currentEventInfo.event.id;
      fetch(`/scitHub/api/calendar/events/${eventId}`, { method: 'DELETE' })
        .then((response) => {
          if (!response.ok) throw new Error('サーバーの応答に失敗しました.');
          return true;
        })
        .then(() => {
          alert('正常に削除されました.');
          calendar.refetchEvents();
          closeModal();
        })
        .catch((error) => {
          console.error('삭제 실패:', error);
          alert('削除に失敗しました.');
        });
    }
  });

  // 폼 제출(등록/수정 공용)
  document.querySelector('#eventForm').addEventListener('submit', function (e) {
    e.preventDefault();

    const formData = new FormData(this);
    const isAllDay = document.getElementById('isAllDay').checked;
    let eventData;

    if (!isAllDay) {
      const startDate = formData.get('eventStartDate') ? formData.get('eventStartDate') + ':00' : null;
      const endDate = formData.get('eventEndDate') ? formData.get('eventEndDate') + ':00' : null;

      eventData = {
        title: formData.get('eventTitle'),
        start: startDate,
        end: endDate,
        content: formData.get('eventContent'),
        allDay: formData.get('isAllDay') === 'on'
      };
    } else {
      const startDateStr = formData.get('startDate');
      let endDateStr = formData.get('endDate') || startDateStr;

      const endDate = new Date(endDateStr + 'T00:00:00');
      endDate.setDate(endDate.getDate() + 1);

      const year = endDate.getFullYear();
      const month = String(endDate.getMonth() + 1).padStart(2, '0');
      const day = String(endDate.getDate()).padStart(2, '0');
      const endDateDay = `${year}-${month}-${day}`;

      eventData = {
        title: formData.get('eventTitle'),
        start: startDateStr + 'T00:00:00',
        end: endDateDay + 'T00:00:00',
        content: formData.get('eventContent'),
        allDay: formData.get('isAllDay') === 'on'
      };
    }

    const eventId = formData.get('eventId');

    // 수정
    if (eventId) {
      fetch(`/scitHub/api/calendar/events/${eventId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(eventData)
      })
        .then((response) => {
          if (!response.ok) throw new Error('サーバーの応答に失敗しました.');
          return response.json();
        })
        .then(() => {
          alert('スケジュールは正常に修正されました.');
          calendar.refetchEvents();
          closeModal();
        })
        .catch((error) => {
          console.error('수정 실패:', error);
          alert('修正に失敗しました.');
        });
      return;
    }

    // 생성
    fetch('/scitHub/api/calendar/events', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(eventData)
    })
      .then((response) => {
        if (response.ok) {
          const location = response.headers.get('Location');
          return response.json().then((createdEvent) => ({ location, createdEvent }));
        }
        return response.json().then((errorData) => Promise.reject(errorData));
      })
      .then(() => {
        calendar.refetchEvents();
        closeModal();
        document.querySelector('#eventForm').reset();
        toggleDateInputs();
      })
      .catch((error) => {
        console.error('생성 실패:', error);
        alert('エラー発生: ' + error.message);
      });
  });
});
