package jp.co.sss.lms.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.entity.TStudentAttendance;
import jp.co.sss.lms.enums.AttendanceStatusEnum;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.DailyAttendanceForm;
import jp.co.sss.lms.mapper.TStudentAttendanceMapper;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.DateUtil;
import jp.co.sss.lms.util.LoginUserUtil;
import jp.co.sss.lms.util.MessageUtil;
import jp.co.sss.lms.util.TrainingTime;

/**
 * 勤怠情報（受講生入力）サービス
 * 
 * @author 東京ITスクール
 */
@Service
public class StudentAttendanceService {

	@Autowired
	private DateUtil dateUtil;
	@Autowired
	private AttendanceUtil attendanceUtil;
	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	private LoginUserUtil loginUserUtil;
	@Autowired
	private LoginUserDto loginUserDto;
	@Autowired
	private TStudentAttendanceMapper tStudentAttendanceMapper;

	/**
	 * 勤怠一覧情報取得
	 * 
	 * @param courseId
	 * @param lmsUserId
	 * @return 勤怠管理画面用DTOリスト
	 */
	public List<AttendanceManagementDto> getAttendanceManagement(Integer courseId,
			Integer lmsUserId) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = tStudentAttendanceMapper
				.getAttendanceManagement(courseId, lmsUserId, Constants.DB_FLG_FALSE);
		for (AttendanceManagementDto dto : attendanceManagementDtoList) {
			// 中抜け時間を設定
			if (dto.getBlankTime() != null) {
				TrainingTime blankTime = attendanceUtil.calcBlankTime(dto.getBlankTime());
				dto.setBlankTimeValue(String.valueOf(blankTime));
			}
			// 遅刻早退区分判定
			AttendanceStatusEnum statusEnum = AttendanceStatusEnum.getEnum(dto.getStatus());
			if (statusEnum != null) {
				dto.setStatusDispName(statusEnum.name);
			}
		}

		return attendanceManagementDtoList;
	}

	/**
	 * 出退勤更新前のチェック
	 * 
	 * @param attendanceType
	 * @return エラーメッセージ
	 */
	public String punchCheck(Short attendanceType) {
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 権限チェック
		if (!loginUserUtil.isStudent()) {
			return messageUtil.getMessage(Constants.VALID_KEY_AUTHORIZATION);
		}
		// 研修日チェック
		if (!attendanceUtil.isWorkDay(loginUserDto.getCourseId(), trainingDate)) {
			return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_NOTWORKDAY);
		}
		// 登録情報チェック
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		switch (attendanceType) {
		case Constants.CODE_VAL_ATWORK:
			if (tStudentAttendance != null
					&& !tStudentAttendance.getTrainingStartTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			break;
		case Constants.CODE_VAL_LEAVING:
			if (tStudentAttendance == null
					|| tStudentAttendance.getTrainingStartTime().equals("")) {
				// 出勤情報がないため退勤情報を入力出来ません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY);
			}
			if (!tStudentAttendance.getTrainingEndTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			TrainingTime trainingStartTime = new TrainingTime(
					tStudentAttendance.getTrainingStartTime());
			TrainingTime trainingEndTime = new TrainingTime();
			if (trainingStartTime.compareTo(trainingEndTime) > 0) {
				// 退勤時刻は出勤時刻より後でなければいけません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE);
			}
			break;
		}
		return null;
	}

	/**
	 * 出勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchIn() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 現在の研修時刻
		TrainingTime trainingStartTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				null);
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		if (tStudentAttendance == null) {
			// 登録処理
			tStudentAttendance = new TStudentAttendance();
			tStudentAttendance.setLmsUserId(loginUserDto.getLmsUserId());
			tStudentAttendance.setTrainingDate(trainingDate);
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setTrainingEndTime("");
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setNote("");
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setFirstCreateDate(date);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendance.setBlankTime(null);
			tStudentAttendanceMapper.insert(tStudentAttendance);
		} else {
			// 更新処理
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendanceMapper.update(tStudentAttendance);
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 退勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchOut() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		// 出退勤時刻
		TrainingTime trainingStartTime = new TrainingTime(
				tStudentAttendance.getTrainingStartTime());
		TrainingTime trainingEndTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				trainingEndTime);
		// 更新処理
		tStudentAttendance.setTrainingEndTime(trainingEndTime.toString());
		tStudentAttendance.setStatus(attendanceStatusEnum.code);
		tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
		tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
		tStudentAttendance.setLastModifiedDate(date);
		tStudentAttendanceMapper.update(tStudentAttendance);
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 勤怠フォームへ設定
	 * 
	 * @param attendanceManagementDtoList
	 * @return 勤怠編集フォーム
	 */
	public AttendanceForm setAttendanceForm(
			List<AttendanceManagementDto> attendanceManagementDtoList) {

		AttendanceForm attendanceForm = new AttendanceForm();
		attendanceForm.setAttendanceList(new ArrayList<DailyAttendanceForm>());
		attendanceForm.setLmsUserId(loginUserDto.getLmsUserId());
		attendanceForm.setUserName(loginUserDto.getUserName());
		attendanceForm.setLeaveFlg(loginUserDto.getLeaveFlg());
		attendanceForm.setBlankTimes(attendanceUtil.setBlankTime());
		/**
		 * @author VU HONG DUC_Task.26
		 * 時間、分マップ設定
		 */
		attendanceForm.setHourMap(attendanceUtil.setHourMap());
		attendanceForm.setMinuteMap(attendanceUtil.setMinuteMap());
		// 途中退校している場合のみ設定
		if (loginUserDto.getLeaveDate() != null) {
			attendanceForm
					.setLeaveDate(dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy-MM-dd"));
			attendanceForm.setDispLeaveDate(
					dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy年M月d日"));
		}

		// 勤怠管理リストの件数分、日次の勤怠フォームに移し替え
		for (AttendanceManagementDto attendanceManagementDto : attendanceManagementDtoList) {
			DailyAttendanceForm dailyAttendanceForm = new DailyAttendanceForm();
			dailyAttendanceForm
					.setStudentAttendanceId(attendanceManagementDto.getStudentAttendanceId());
			dailyAttendanceForm
					.setTrainingDate(dateUtil.toString(attendanceManagementDto.getTrainingDate()));
			dailyAttendanceForm
					.setTrainingStartTime(attendanceManagementDto.getTrainingStartTime());
			dailyAttendanceForm.setTrainingEndTime(attendanceManagementDto.getTrainingEndTime());
			/**
			 * 出退勤時間を抜き出す
			 * @author VU HONG DUC_Task.26
			 */
			LinkedHashMap<Integer, String> hourMap = attendanceUtil.setHourMap();
			LinkedHashMap<Integer, String> minuteMap = attendanceUtil.setMinuteMap();
			String startTime = attendanceManagementDto.getTrainingStartTime();
			String endTime = attendanceManagementDto.getTrainingEndTime();
			/**出勤時間を抜き出す*/
			if (startTime != null && !startTime.isBlank()) {
				Integer startHour = attendanceUtil.getHour(dailyAttendanceForm.getTrainingStartTime());
				Integer startMinute = attendanceUtil.getMinute(dailyAttendanceForm.getTrainingStartTime());
				dailyAttendanceForm.setTrainingStartHour(hourMap.get(startHour));
				dailyAttendanceForm.setTrainingStartMinute(minuteMap.get(startMinute));
			}
			/**退勤時間を抜き出す*/
			if (endTime != null && !endTime.isBlank()) {
				Integer endHour = attendanceUtil.getHour(dailyAttendanceForm.getTrainingEndTime());
				Integer endMinute = attendanceUtil.getMinute(dailyAttendanceForm.getTrainingEndTime());
				dailyAttendanceForm.setTrainingEndHour(hourMap.get(endHour));
				dailyAttendanceForm.setTrainingEndMinute(minuteMap.get(endMinute));
			}

			if (attendanceManagementDto.getBlankTime() != null) {
				dailyAttendanceForm.setBlankTime(attendanceManagementDto.getBlankTime());
				dailyAttendanceForm.setBlankTimeValue(String.valueOf(
						attendanceUtil.calcBlankTime(attendanceManagementDto.getBlankTime())));
			}
			dailyAttendanceForm.setStatus(String.valueOf(attendanceManagementDto.getStatus()));
			dailyAttendanceForm.setNote(attendanceManagementDto.getNote());
			dailyAttendanceForm.setSectionName(attendanceManagementDto.getSectionName());
			dailyAttendanceForm.setIsToday(attendanceManagementDto.getIsToday());
			dailyAttendanceForm.setDispTrainingDate(dateUtil
					.dateToString(attendanceManagementDto.getTrainingDate(), "yyyy年M月d日(E)"));
			dailyAttendanceForm.setStatusDispName(attendanceManagementDto.getStatusDispName());

			attendanceForm.getAttendanceList().add(dailyAttendanceForm);
		}

		return attendanceForm;
	}

	/**
	 * 勤怠登録・更新処理
	 * 
	 * @param attendanceForm
	 * @return 完了メッセージ
	 * @throws ParseException
	 */
	public String update(AttendanceForm attendanceForm) throws ParseException {

		Integer lmsUserId = loginUserUtil.isStudent() ? loginUserDto.getLmsUserId()
				: attendanceForm.getLmsUserId();

		// 現在の勤怠情報（受講生入力）リストを取得
		List<TStudentAttendance> tStudentAttendanceList = tStudentAttendanceMapper
				.findByLmsUserId(lmsUserId, Constants.DB_FLG_FALSE);

		// 入力された情報を更新用のエンティティに移し替え
		Date date = new Date();
		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {

			// 更新用エンティティ作成
			TStudentAttendance tStudentAttendance = new TStudentAttendance();
			// 日次勤怠フォームから更新用のエンティティにコピー
			BeanUtils.copyProperties(dailyAttendanceForm, tStudentAttendance);
			// 研修日付
			tStudentAttendance
					.setTrainingDate(dateUtil.parse(dailyAttendanceForm.getTrainingDate()));
			// 現在の勤怠情報リストのうち、研修日が同じものを更新用エンティティで上書き
			for (TStudentAttendance entity : tStudentAttendanceList) {
				if (entity.getTrainingDate().equals(tStudentAttendance.getTrainingDate())) {
					tStudentAttendance = entity;
					break;
				}
			}
			tStudentAttendance.setLmsUserId(lmsUserId);
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			// 出勤時刻整形
			TrainingTime trainingStartTime = null;
			trainingStartTime = new TrainingTime(dailyAttendanceForm.getTrainingStartTime());
			tStudentAttendance.setTrainingStartTime(trainingStartTime.getFormattedString());
			// 退勤時刻整形
			TrainingTime trainingEndTime = null;
			trainingEndTime = new TrainingTime(dailyAttendanceForm.getTrainingEndTime());
			tStudentAttendance.setTrainingEndTime(trainingEndTime.getFormattedString());
			// 中抜け時間
			tStudentAttendance.setBlankTime(dailyAttendanceForm.getBlankTime());
			// 遅刻早退ステータス
			if ((trainingStartTime != null || trainingEndTime != null)
					&& !dailyAttendanceForm.getStatusDispName().equals("欠席")) {
				AttendanceStatusEnum attendanceStatusEnum = attendanceUtil
						.getStatus(trainingStartTime, trainingEndTime);
				tStudentAttendance.setStatus(attendanceStatusEnum.code);
			}
			// 備考
			tStudentAttendance.setNote(dailyAttendanceForm.getNote());
			// 更新者と更新日時
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			// 削除フラグ
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			// 登録用Listへ追加
			tStudentAttendanceList.add(tStudentAttendance);
		}
		// 登録・更新処理
		for (TStudentAttendance tStudentAttendance : tStudentAttendanceList) {
			if (tStudentAttendance.getStudentAttendanceId() == null) {
				tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
				tStudentAttendance.setFirstCreateDate(date);
				tStudentAttendanceMapper.insert(tStudentAttendance);
			} else {
				tStudentAttendanceMapper.update(tStudentAttendance);
			}
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 勤怠過去未入力の有無チェック
	 * @author VU HONG DUC_Task25
	 * @return attendanceNotEnteredFlag
	 */
	public boolean attendanceNotEnteredCheck() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String todayStr = sdf.format(new Date());
		Date today = java.sql.Date.valueOf(todayStr);
		//勤怠過去日未入力件数の取得
		int attendanceNotEnteredCount = tStudentAttendanceMapper.countByNullTrainingStartTimeOrTrainingEndTime(
				loginUserDto.getLmsUserId(), Constants.DB_FLG_FALSE, today);
		//勤怠過去日未入力チェック有無フラグ
		boolean attendanceNotEnteredFlag;
		if (attendanceNotEnteredCount > 0) {
			attendanceNotEnteredFlag = true;
		} else {
			attendanceNotEnteredFlag = false;
		}
		return attendanceNotEnteredFlag;
	}

	/**
	 * 出勤/退勤時間をhh:mm形式に設定
	 * @author VU HONG DUC_Task.26
	 * @param attendanceForm
	 */
	public void setAttendanceTime(AttendanceForm attendanceForm) {
		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {
			String trainingStartHour = dailyAttendanceForm.getTrainingStartHour();
			String trainingStartMinute = dailyAttendanceForm.getTrainingStartMinute();
			String trainingStartTime = trainingStartHour + trainingStartMinute;
			dailyAttendanceForm.setTrainingStartTime(trainingStartTime);
			//退勤
			String trainingEndHour = dailyAttendanceForm.getTrainingEndHour();
			String trainingEndMinute = dailyAttendanceForm.getTrainingEndMinute();
			String trainingEndTime = trainingEndHour + trainingEndMinute;
			dailyAttendanceForm.setTrainingEndTime(trainingEndTime);
		}
	}

	public List<String> validation(List<DailyAttendanceForm> attendanceList, BindingResult result,
			AttendanceForm attendanceForm) {
		int i = 0;
		boolean maxLengthFlg = false;
		boolean trainingTimeRangeFlg = false;
		boolean inputInvalidStartTimeFlg = false;
		boolean inputInvalidEndTimeFlg = false;
		boolean punchInEmptyFlg = false;
		boolean priorityFlg = false;
		List<Integer> iWrapperList = new ArrayList<>();
		for (DailyAttendanceForm dailyAttendanceForm : attendanceList) {
			//備考欄の文字数チェック
			String note = dailyAttendanceForm.getNote();
			int notCount = note.length();
			if (notCount > 100) {
				result.addError(new FieldError(result.getObjectName(), "attendanceList[" + i + "].note",
						messageUtil.getMessage(Constants.VALID_KEY_MAXBYTELENGTH, new String[] { "備考", "100" })));
				maxLengthFlg = true;
			}
			String trainingStartTime = dailyAttendanceForm.getTrainingStartTime();
			String trainingEndTime = dailyAttendanceForm.getTrainingEndTime();
			TrainingTime checkTime = new TrainingTime();
			boolean isStartTime = checkTime.isValidTrainingTime(trainingStartTime);
			boolean isEndTime = checkTime.isValidTrainingTime(trainingEndTime);
			//出勤時間の時分が入力されているかをチェック
			if (isStartTime == false) {
				String trainingStartHour = dailyAttendanceForm.getTrainingStartHour();
				String trainingStartMinute = dailyAttendanceForm.getTrainingStartMinute();
				if (trainingStartHour.isBlank()) {
					new FieldError(result.getObjectName(), "attendanceList[" + i + "].trainingStartHour",
							messageUtil.getMessage(Constants.INPUT_INVALID, new String[] { "出勤時間" }));
				} else if (trainingStartMinute.isBlank()) {
					new FieldError(result.getObjectName(), "attendanceList[" + i + "].trainingStartMinute",
							messageUtil.getMessage(Constants.INPUT_INVALID, new String[] { "出勤時間" }));
				}
				inputInvalidStartTimeFlg = true;
				priorityFlg = true;
			}
			//退勤時間のジフンが入力されているかをチェック
			if (isEndTime == false) {
				String trainingEndHour = dailyAttendanceForm.getTrainingEndHour();
				String trainingEndMinute = dailyAttendanceForm.getTrainingEndMinute();
				if (trainingEndHour.isBlank()) {
					new FieldError(result.getObjectName(), "attendanceList[" + i + "].trainingEndHour",
							messageUtil.getMessage(Constants.INPUT_INVALID, new String[] { "退勤時間" }));
				} else if (trainingEndMinute.isBlank()) {
					new FieldError(result.getObjectName(), "attendanceList[" + i + "].trainingEndMinute",
							messageUtil.getMessage(Constants.INPUT_INVALID, new String[] { "退勤時間" }));
				}
				inputInvalidEndTimeFlg = true;
			}
			//出勤時間無し、退勤時間ありの場合
			if (trainingStartTime.isBlank() && !trainingEndTime.isBlank()) {
				result.addError(new FieldError(result.getObjectName(), "attendanceList[" + i + "].trainingStartHour",
						messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY)));
				result.addError(new FieldError(result.getObjectName(), "attendanceList[" + i + "].traininEndHour",
						messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY)));
				punchInEmptyFlg = true;
			}
		}
		//エラー文を統一化
		List<String> errorBox = new ArrayList<>();
		if (maxLengthFlg) {
			errorBox.add(messageUtil.getMessage(Constants.VALID_KEY_MAXLENGTH, new String[] { "備考", "100" }));
		}
		if(inputInvalidStartTimeFlg) {
			errorBox.add(messageUtil.getMessage(Constants.INPUT_INVALID, new String[] {"出勤時間"}));
		}
		if(inputInvalidEndTimeFlg) {
			errorBox.add(messageUtil.getMessage(Constants.INPUT_INVALID, new String[] {"退勤時間"}));
		}
		return errorBox;
	}
}
