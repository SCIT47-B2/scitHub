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

  // 색상 선택 버튼 제어
  const colorTrigger = document.querySelector('#colorPickerTrigger');
  if (isEditable) {
    colorTrigger.style.pointerEvents = 'auto';
    colorTrigger.style.cursor = 'pointer';
  } else {
    colorTrigger.style.pointerEvents = 'none';
    colorTrigger.style.cursor = 'default';
  }
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
    <button type="submit" class="btn btn-primary" id="submitBtn">保存</button>
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

  // 색상 처리 추가
  const eventColor = event.extendedProps.color || '#3788d8';
  const hiddenInput = document.getElementById('eventColor'); 
  const trigger = document.getElementById('colorPickerTrigger');

  document.getElementById('eventColor').value = eventColor;
  document.getElementById('colorPickerTrigger').style.backgroundColor = eventColor;
  
  if (hiddenInput) hiddenInput.value = eventColor;
  if (trigger) trigger.style.backgroundColor = eventColor;
  
  // colorPalette는 Tippy가 관리하므로 직접 조작하지 않음
  // 또는 안전하게 체크 후 처리
  const colorPalette = document.querySelector('#colorPalette');
  if (colorPalette) {
    const currentSelected = colorPalette.querySelector('.selected');
    if (currentSelected) {
      currentSelected.classList.remove('selected');
    }
    const newSelectedSwatch = colorPalette.querySelector(`.color-swatch[data-color="${eventColor}"]`);
    if (newSelectedSwatch) {
      newSelectedSwatch.classList.add('selected');
    }
  }

  toggleDateInputs();

  if (event.allDay) {
    document.getElementById('startDate').value = event.startStr;
    document.getElementById('endDate').value = event.endStr
      ? new Date(new Date(event.endStr).getTime() - 86400000).toISOString().split('T')[0]
      : event.startStr;
  } else {
    // 시간대 변환 없이 YYYY-MM-DDTHH:mm 형식으로 직접 만드는 헬퍼 함수
    const toLocalISOString = (date) => {
        if (!date) return ''; // 날짜가 없으면 빈 문자열 반환
        
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        
        return `${year}-${month}-${day}T${hours}:${minutes}`;
    };

    // 새로 만든 함수를 사용해서 입력창의 값을 설정
    document.getElementById('eventStartDate').value = toLocalISOString(event.start);
    document.getElementById('eventEndDate').value = toLocalISOString(event.end);
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


      // 색상 초기화 추가
      document.getElementById('eventColor').value = '#3788d8';
      document.getElementById('colorPickerTrigger').style.backgroundColor = '#3788d8';

      const colorPalette = document.getElementById('colorPalette');
      if (colorPalette) {  // ⭐ null 체크 필수!
        const currentSelected = colorPalette.querySelector('.selected');
        if (currentSelected) currentSelected.classList.remove('selected');
        const firstSwatch = colorPalette.querySelector('.color-swatch[data-color="#3788d8"]');
        if (firstSwatch) firstSwatch.classList.add('selected');
      }

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