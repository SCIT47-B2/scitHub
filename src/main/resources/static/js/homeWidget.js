const currentScript = document.currentScript;
const ddayPageUrl = currentScript.dataset.ddayUrl;

document.addEventListener('DOMContentLoaded', function() {
    initializeDdayWidget();
    initializeCalendarWidget();
});

// ==========================================================
// D-Day 위젯 관련 함수들
// ==========================================================
async function initializeDdayWidget() {
    const widget = document.getElementById('ddayWidget');
    const modal = document.getElementById('ddaySelectModal');
    const closeModalBtn = document.getElementById('closeDdayModal');
    const saveBtn = document.getElementById('savePinnedDdays');

    const allDdays = await fetchAllDdays();
    if (allDdays) {
        renderDdayWidget(allDdays);
        renderDdayModal(allDdays);
    }

    widget.addEventListener('click', () => modal.style.display = 'flex');
    closeModalBtn.addEventListener('click', () => modal.style.display = 'none');
    modal.addEventListener('click', (e) => {
        if (e.target === modal) modal.style.display = 'none';
    });
    saveBtn.addEventListener('click', () => savePinnedDdays(allDdays));
}

async function fetchAllDdays() {
    try {
        const response = await fetch('/scitHub/api/ddays');

        if (!response.ok) throw new Error('D-Day data fetch failed');
        return await response.json();
    } catch (error) {
        console.error(error);
        return []; // 실패 시 빈 배열 반환
    }
}

function renderDdayWidget(ddayEvents) {
    const widget = document.getElementById('ddayWidget');
    const allDdays = ddayEvents || [];
    const pinnedDdays = allDdays.filter(d => d.pinned);   // .slice(0, 2) 이걸로 표시되는 D-Day 갯수 조절

    widget.style.display = 'flex';

    if (pinnedDdays.length > 0) {
        widget.innerHTML = pinnedDdays.map(dday => {
            const { ddayString } = calculateDday(dday.dday);

            // 카운터와 제목을 각각의 span 태그로 감싸줍니다.
            return `<div class="dday-item">
                        <span class="dday-counter">${ddayString}</span>
                        <span class="dday-title">${dday.title}</span>
                    </div>`;
        }).join('');
    } else if (allDdays.length > 0) {
        widget.innerHTML = `<div class="dday-item-empty">クリックしてホームに表示する<br>D-Dayを選択します。</div>`;
    } else {
        widget.innerHTML = `<div class="dday-item-empty">D-Dayを登録してください。</div>`;
    }
}

function renderDdayModal(ddayEvents) {
    const listEl = document.getElementById('ddayList');
    const emptyEl = document.getElementById('ddayEmptyState');
    const footerEl = document.getElementById('ddayModalFooter');

    if (!ddayEvents || ddayEvents.length === 0) {
        listEl.style.display = 'none';
        footerEl.style.display = 'none';
        emptyEl.style.display = 'block';
        return;
    }

    listEl.style.display = 'block';
    footerEl.style.display = 'block';
    emptyEl.style.display = 'none';

    listEl.innerHTML = ddayEvents.map(dday => {
        const { ddayString, targetDate } = calculateDday(dday.dday);
        const formattedDate = targetDate.toLocaleDateString('ja-JP');

        const editUrl = `${ddayPageUrl}?action=edit&id=${dday.ddayId}`;

        return `<li>
                    <input type="checkbox" data-id="${dday.ddayId}" ${dday.pinned ? 'checked' : ''}>
                    <div class="dday-info">
                        <a class="dday-title" href="${editUrl}">${dday.title}</a>
                        <div class="dday-date">${formattedDate}</div>
                    </div>
                    <div class="dday-counter">${ddayString}</div>
                </li>`;
    }).join('');
}

// 선택한 D-Day를 저장하는 함수
async function savePinnedDdays(allDdays) {
    const checkedIds = Array.from(document.querySelectorAll('#ddayList input:checked')).map(box => box.dataset.id);
    try {
        const response = await fetch('/scitHub/api/dday/pin', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(checkedIds)
        });
        if (!response.ok) throw new Error('Failed to save');
        alert('保存しました。');
        document.getElementById('ddaySelectModal').style.display = 'none';

        const updatedDdays = allDdays.map(d => ({ ...d, pinned: checkedIds.includes(String(d.ddayId)) }));
        renderDdayWidget(updatedDdays);
        renderDdayModal(updatedDdays);
    } catch (error) {
        console.error(error);
        alert('エラーが発生しました。');
    }
}

// 이 함수 하나만 교체하면 두 곳의 문제가 모두 해결됩니다.
function calculateDday(targetDateStr) {
    // 1. null이나 undefined 값이 들어와도 안전하게 처리
    if (!targetDateStr || typeof targetDateStr !== 'string') { 
        return { ddayString: 'D-?', targetDate: new Date() };
    }

    // 2. 시간대 문제 없이 날짜 계산
    const parts = targetDateStr.split('-');
    if (parts.length !== 3) {
        return { ddayString: '날짜 오류', targetDate: new Date() };
    }
    const [year, month, day] = parts.map(Number);
    const targetDate = new Date(year, month - 1, day);

    // 3. 오늘 날짜와 비교
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const diffTime = targetDate.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    let ddayString;
    if (diffDays === 0) {
        ddayString = 'D-Day';
    } else if (diffDays > 0) {
        ddayString = `D-${diffDays}`;
    } else {
        ddayString = `D+${Math.abs(diffDays)}`;
    }

    return { ddayString, targetDate };
}

// ==========================================================
// 캘린더 위젯 관련 함수
// ==========================================================
function initializeCalendarWidget() {
    const calendarEl = document.getElementById('mini-calendar');
    const titleEl = document.getElementById('widget-date-title');
    if (!calendarEl || !titleEl) return;

    // 날짜 제목 클릭 시 캘린더 페이지로 이동
    titleEl.addEventListener('click', function() {
        window.location.href = '/scitHub/calendar/schedule';
    });
    
    const calendar = new FullCalendar.Calendar(calendarEl, {
        locale: 'ja',
        initialView: 'timeGridDay',
        initialDate: new Date(),
        slotEventOverlap: false,  // 이벤트가 겹치지 않게
        eventDisplay: 'block',    // 블록 형태로 표시
        dayMaxEvents: false,      // 이벤트 개수 제한 없음
        headerToolbar: false,
        slotDuration: '01:00:00', // 30분에서 1시간 단위로 변경
        slotLabelInterval: '01:00:00', // 1시간마다 라벨 표시
        allDaySlot: true,
        allDayText: '終日',
        dayHeaders: false,
        height: 'auto',
        contentHeight: 'auto',
        slotMinTime: '08:00:00',
        slotMaxTime: '23:00:00',
        slotLabelFormat: { hour: '2-digit', minute: '2-digit', hour12: false },
        allDayText: '終日', // 'all-day' 텍스트를 '終日'로 변경
        eventClick: function(info) {
            // schedule.js와 동일하게 'view' 모드로 모달 열기
            openModal('view', info);
        },
        events: function(fetchInfo, successCallback, failureCallback) {
            fetch('/scitHub/api/calendar/events?showPublic=true&showPrivate=true')
                .then(res => res.ok ? res.json() : Promise.reject('Server error'))
                .then(data => {
                    const formattedEvents = data.map(event => ({
                        // FullCalendar가 인식할 수 있는 표준 속성
                        id: event.eventId,
                        title: event.title,
                        start: event.start,
                        end: event.end,
                        allDay: event.allDay,
                        color: event.color,  // 이벤트 색상
                        backgroundColor: event.color,  // 배경색도 명시적으로 설정
                        
                        // 추가 데이터는 extendedProps에 저장
                        extendedProps: {
                            content: event.content,
                            visibility: event.visibility,
                            userId: event.userId,
                            color: event.color  // 나중에 사용할 수 있도록 color도 저장
                        }
                    }));
                    console.log("✅ 2단계: FullCalendar에 전달될 최종 가공 데이터", formattedEvents);
                    successCallback(formattedEvents);
                })
                .catch(err => {
                    console.error('Failed to load calendar events:', err);
                    failureCallback(err);
                });
        },
        eventContent: function(arg) {

            // 종일 일정 처리 추가
            if (arg.event.allDay) {
                const title = arg.event.title;
                const startDate = new Date(arg.event.start);
                const endDate = arg.event.end ? new Date(arg.event.end) : null;
                
                let dateStr = '';
                
                if (endDate) {
                    const nextDay = new Date(startDate);
                    nextDay.setDate(nextDay.getDate() + 1);
                    
                    if (endDate.toDateString() === nextDay.toDateString()) {
                        dateStr = `${startDate.getMonth()+1}/${startDate.getDate()}`;
                    } else {
                        const endDateForDisplay = new Date(endDate);
                        endDateForDisplay.setDate(endDateForDisplay.getDate() - 1);
                        dateStr = `${startDate.getMonth()+1}/${startDate.getDate()}-${endDateForDisplay.getMonth()+1}/${endDateForDisplay.getDate()}`;
                    }
                } else {
                    dateStr = `${startDate.getMonth()+1}/${startDate.getDate()}`;
                }
                
                const bgColor = arg.event.backgroundColor || 
                                arg.event.extendedProps.color || 
                                '#3788d8';
                
                return {
                    html: `
                        <div style="
                            background: ${bgColor};
                            color: white;
                            padding: 4px 8px;
                            border-radius: 4px;
                            font-size: 0.75rem;
                            display: flex;
                            align-items: center;
                            justify-content: space-between;
                            width: 100%;
                        ">
                            <span style="
                                font-weight: 600;
                                overflow: hidden;
                                text-overflow: ellipsis;
                                white-space: nowrap;
                                margin-right: 8px;
                            ">${title}</span>
                            <span style="
                                opacity: 0.8;
                                font-size: 0.7rem;
                                flex-shrink: 0;
                            ">${dateStr}</span>
                        </div>
                    `
                };
            }


            const title = arg.event.title;
            const start = arg.event.start;
            const end = arg.event.end;
            const originalStart = new Date(arg.event.start); // 시작일
            const originalEnd = arg.event.end ? new Date(arg.event.end) : null; // 종료일
                                
            // 오늘 날짜
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            const tomorrow = new Date(today);
            tomorrow.setDate(tomorrow.getDate() + 1);

            // 여러 날에 걸친 일정인지 확인
            const isMultiDay = originalEnd && 
                (originalEnd.toDateString() !== originalStart.toDateString());
            
            let timeHtml = '';
            if (isMultiDay) {
                // 여러 날 일정은 전체 기간 표시
                const startDateStr = originalStart.toLocaleDateString('ja-JP', {
                    month: 'numeric',
                    day: 'numeric'
                });
                const endDateStr = originalEnd ? originalEnd.toLocaleDateString('ja-JP', {
                    month: 'numeric',
                    day: 'numeric'
                }) : '';
                
                timeHtml = `<div style="font-size: 0.7rem; opacity: 0.9;">${startDateStr} ~ ${endDateStr}</div>`;
            } else {
                // 하루 일정은 시간만 표시
                const durationMinutes = end ? (end.getTime() - start.getTime()) / 60000 : 60;
                const isShortEvent = durationMinutes <= 60;
                
                if (!isShortEvent) {
                    const startTime = start.toLocaleTimeString('ja-JP', {
                        hour: '2-digit',
                        minute: '2-digit',
                        hour12: false
                    });
                    const endTime = end ? end.toLocaleTimeString('ja-JP', {
                        hour: '2-digit',
                        minute: '2-digit',
                        hour12: false
                    }) : '';
                    
                    timeHtml = `<div style="font-size: 0.7rem; opacity: 0.9;">${startTime} - ${endTime}</div>`;
                }
            }

            // 색상 결정
            const bgColor = arg.event.backgroundColor || 
                            arg.event.extendedProps.color || 
                            '#3788d8';
            console.log(`@@@@@ : ${bgColor}`);
            
            // HTML 생성
            return {
                html: `
                    <div style="
                        background: ${bgColor};
                        color: white; 
                        padding: 8px 12px; 
                        border-radius: 8px;
                        height: 100%;
                        display: flex;
                        flex-direction: column;
                        justify-content: center;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    ">
                        <div style="
                            font-weight: 600;
                            font-size: 0.85rem;
                            overflow: hidden;
                            text-overflow: ellipsis;
                            white-space: nowrap;
                        ">${title}</div>
                        ${timeHtml}
                    </div>
                `
            };
        },
        viewDidMount: function(info) {
            const currentDate = calendar.getDate();
            titleEl.textContent = currentDate.toLocaleDateString('ja-JP', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' });
        }
    });
    calendar.render();

    // 모달에 필요한 요소들을 가져옵니다.
    const eventModal = document.querySelector('#eventModal');
    const modalButtons = document.querySelector('#modalButtons');
    const eventForm = document.querySelector('#eventForm');
    const trigger = document.querySelector('#colorPickerTrigger');
    const colorPalette = document.querySelector('#colorPalette');
    const hiddenColorInput = document.querySelector('#eventColor');

    // Tippy.js (색상 선택기) 초기화
    if (trigger && colorPalette) {
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
                hiddenColorInput.value = newColor;
                trigger.style.backgroundColor = newColor;

                const currentSelected = colorPalette.querySelector('.selected');
                if (currentSelected) currentSelected.classList.remove('selected');
                e.target.classList.add('selected');
                tippyInstance.hide();
            }
        });
    }

    // 모달 바깥 클릭 시 닫기 이벤트
    if (eventModal) {

        addDateValidationListeners();


        const isAllDayCheckbox = document.getElementById('isAllDay');
        if (isAllDayCheckbox) {
            isAllDayCheckbox.addEventListener('change', function() {
                window.toggleDateInputs();
            });
        }
        eventModal.addEventListener('click', function(event) {
            if (event.target === eventModal) window.closeModal();
        });
    }

    // 수정/삭제 버튼 클릭 이벤트
    if (modalButtons) {
        modalButtons.addEventListener('click', function(event) {
            const clicked = event.target;
            if (clicked.id === 'editBtn') {
                window.enableEditMode();
            }
            if (clicked.id === 'deleteBtn') {
                if (!confirm('本当にこのスケジュールを削除しますか?')) return;
                const eventId = window.currentEventInfo.event.id;
                fetch(`/scitHub/api/calendar/events/${eventId}`, { method: 'DELETE' })
                    .then(response => {
                        if (!response.ok) throw new Error('삭제 실패');
                        alert('正常に削除されました。');
                        calendar.refetchEvents(); // ✨ 홈 캘린더 새로고침
                        window.closeModal();
                    })
                    .catch(error => alert('削除に失敗しました。'));
            }
        });
    }

    // 폼 제출(수정) 이벤트
    if (eventForm) {
        eventForm.addEventListener('submit', function(e) {
            e.preventDefault();
            const eventId = document.getElementById('eventId').value;
            if (!eventId) return; // 홈에서는 수정만 처리

            // ... (schedule.js의 eventData 만드는 로직과 동일) ...
            const formData = new FormData(this);
            const isAllDay = document.getElementById('isAllDay').checked;
            let eventData;
            if (!isAllDay) {
                // ... 시간 포함 일정 데이터 생성 ...
                const startDate = formData.get('eventStartDate') ? formData.get('eventStartDate') + ':00' : null;
                const endDate = formData.get('eventEndDate') ? formData.get('eventEndDate') + ':00' : null;

                eventData = {
                    title: formData.get('eventTitle'),
                    start: startDate,
                    end: endDate,
                    content: formData.get('eventContent'),
                    allDay: false, // isAllDay가 false이므로 직접 값을 넣어줍니다.
                    color: formData.get('eventColor')
                };
            } else {
                // ... 종일 일정 데이터 생성 ...
                const startDateStr = formData.get('startDate');
                let endDateStr = formData.get('endDate') || startDateStr;

                // FullCalendar는 종일 일정의 종료일을 다음 날 자정으로 인식하므로,
                // 저장할 때는 다음 날로 계산해서 보내줍니다.
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
                    allDay: true, // isAllDay가 true이므로 직접 값을 넣어줍니다.
                    color: formData.get('eventColor')
                };
            }

            // fetch로 수정 요청 보내기
            fetch(`/scitHub/api/calendar/events/${eventId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(eventData)
            })
            .then(response => {
                if (!response.ok) throw new Error('수정 실패');
                alert('スケジュールが修正されました。');
                calendar.refetchEvents(); // 홈 캘린더 새로고침
                window.closeModal();
            })
            .catch(error => alert('修正に失敗しました。'));
        });
    }
}