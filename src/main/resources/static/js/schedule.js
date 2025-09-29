// /** 스케줄 페이지 스크립트
/** 필터(전체/개인) 변경 시 이벤트 재조회 */
function handleFilterChange(calendar) {
  if (calendar) calendar.refetchEvents();
}



/** 메인 초기화 */
document.addEventListener('DOMContentLoaded', function onReady() {
  // 공용 참조
  const eventModal = document.querySelector('#eventModal');
  const modalButtons = document.querySelector('#modalButtons');
  // === 색상 선택 기능 초기화 START ===
  const trigger = document.querySelector('#colorPickerTrigger');
  const colorPalette = document.querySelector('#colorPalette');
  const hiddenColorInput = document.querySelector('#eventColor');

  const tippyInstance = tippy(trigger, {
    content: colorPalette,
    allowHTML: true,
    interactive: true,
    trigger: 'click',
    placement: 'bottom-end',
    appendTo: () => document.body,
  });

  colorPalette.addEventListener('click', function(e) {
    if (e.target.classList.contains('color-swatch')) {
      const newColor = e.target.dataset.color;
      
      // 1. 숨겨진 input 값 변경
      hiddenColorInput.value = newColor;
      // 2. 모달 헤더의 동그라미 색 변경
      trigger.style.backgroundColor = newColor;

      // 3. 팔레트 내 선택 상태 변경
      const currentSelected = colorPalette.querySelector('.selected');
      if (currentSelected) currentSelected.classList.remove('selected');
      e.target.classList.add('selected');

      // 4. 팝업 닫기
      tippyInstance.hide();
    }
  });

  addDateValidationListeners();

  const isAllDayCheckbox = document.getElementById('isAllDay');
  if (isAllDayCheckbox) {
    isAllDayCheckbox.addEventListener('change', function() {
      console.log('종일 체크박스 변경됨:', this.checked);
      toggleDateInputs();
    });
  }
  
  const calendarEl = document.getElementById('calendar');
  const calendar = new FullCalendar.Calendar(calendarEl, {
    initialView: 'dayGridMonth',
    locale: 'ja',
    editable: true,
    headerToolbar: { left: '', center: '', right: '' },
    buttonText: { today: 'Today', month: 'M', week: 'W', day: 'D' },
    eventDisplay: 'block',
    allDayText: '終日',

        // 종일 일정 관련
    allDayMaintainDuration: true,
    dayMaxEvents: false,
    dayMaxEventRows: false,

    dayHeaderContent: function (arg) {
      const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
      return dayNames[arg.date.getDay()];
    },
    // 이벤트 안에 표시되는 시간 포맷 (월/주/일 뷰 모두 적용)
    eventTimeFormat: {
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
    },

    selectable: true,
    firstDay: 0,
    fixedWeekCount: false,

    // 렌더링 후 스크롤 제거
    viewDidMount: function() {
        // 헤더와 종일 영역 스크롤 제거
        document.querySelectorAll('#mini-calendar .fc-scrollgrid-section-header .fc-scroller, #mini-calendar .fc-daygrid-body .fc-scroller').forEach(el => {
            el.style.overflow = 'visible';
            el.style.overflowY = 'visible';
            el.style.overflowX = 'visible';
        });
    },

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
                userId: event.userId,
                color: event.color
              },
              editable: isEventEditable,
              color: event.color || '#3788d8'
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
    // 제목 입력했는지 유효성 검사
    const titleInput = document.getElementById('eventTitle');
    if (titleInput.value.trim() === '') {
      alert('タイトルを入力してください。');
      titleInput.focus();
      return;
    }

    const formData = new FormData(this);

    // 디버깅: color 값이 제대로 있는지 확인
    console.log('Color input value:', document.getElementById('eventColor').value);
    console.log('FormData color:', formData.get('eventColor'));

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
        allDay: formData.get('isAllDay') === 'on',
        color: formData.get('eventColor')
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
        allDay: formData.get('isAllDay') === 'on',
        color: formData.get('eventColor')
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
          alert('スケジュールが修正されました.');
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

        
      // 1. errorData 객체에서 실제 오류 메시지(value)들을 추출합니다.
      const errorMessages = Object.values(error);

      // 2. 만약 메시지가 있다면 첫 번째 메시지를, 없다면 기본 메시지를 사용합니다.
      const displayMessage = errorMessages.length > 0 ? errorMessages[0] : '登録中にエラーが発生しました。';

      // 3. 사용자에게 의미 있는 오류 메시지를 보여줍니다.
      alert(displayMessage);
      });
  });
});
