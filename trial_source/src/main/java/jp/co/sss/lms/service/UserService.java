package jp.co.sss.lms.service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.lms.dto.CompanyDto;
import jp.co.sss.lms.dto.DailyReportDto;
import jp.co.sss.lms.dto.ExamResultDto;
import jp.co.sss.lms.dto.LmsUserDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.dto.UserDetailDto;
import jp.co.sss.lms.entity.MPlace;
import jp.co.sss.lms.entity.MUser;
import jp.co.sss.lms.mapper.MCompanyMapper;
import jp.co.sss.lms.mapper.MLmsUserMapper;
import jp.co.sss.lms.mapper.MPlaceMapper;
import jp.co.sss.lms.mapper.MUserMapper;
import jp.co.sss.lms.mapper.TDailyReportSubmitMapper;
import jp.co.sss.lms.mapper.TExamResultMapper;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.LoginUserUtil;

/**
 * ユーザー情報サービス
 * 
 * @author 東京ITスクール
 */
@Service
public class UserService {

	@Autowired
	private LoginUserDto loginUserDto;
	@Autowired
	private LoginUserUtil loginUserUtil;
	@Autowired
	private HttpSession session;
	@Autowired
	private MLmsUserMapper mLmsUserMapper;
	@Autowired
	private MUserMapper mUserMapper;
	@Autowired
	private TDailyReportSubmitMapper tDailyReportSubmitMapper;
	@Autowired
	private TExamResultMapper tExamResultMapper;
	@Autowired
	private MPlaceMapper mPlaceMapper;
	@Autowired
	private MCompanyMapper mCompanyMapper;

	/**
	 * セキュリティ同意フラグ登録
	 */
	public void updateSecurityFlg() {
		Date today = new Date();
		MUser mUser = mUserMapper.findByUserId(loginUserDto.getUserId(), Constants.DB_FLG_FALSE);
		mUser.setSecurityAgreeFlg(Constants.CODE_VAL_SECURITY_AGREE);
		mUser.setLastModifiedUser(loginUserDto.getUserId());
		mUser.setLastModifiedDate(today);
		boolean updateFlg = mUserMapper.updateSecrityFlg(mUser);
		if (updateFlg) {
			loginUserDto.setSecurityAgreeFlg(Constants.CODE_VAL_SECURITY_AGREE);
			session.setAttribute("loginUserDto", loginUserDto);
		}
	}

	/**
	 * ユーザー詳細DTOの取得
	 * 
	 * @return ユーザー詳細DTO
	 */
	public LmsUserDto getUserDetail(Integer lmsUserId) {

		lmsUserId = loginUserUtil.isStudent() ? loginUserDto.getLmsUserId() : lmsUserId;
		LmsUserDto lmsUserDto = new LmsUserDto();

		UserDetailDto userDetailDto = mLmsUserMapper.getUserDetail(lmsUserId,
				Constants.DB_FLG_FALSE);
		lmsUserDto.setUserDetailDto(userDetailDto);

		List<ExamResultDto> examResultDtoList = tExamResultMapper.getExamResultDto(lmsUserId,
				loginUserDto.getAccountId(), Constants.DB_FLG_FALSE);
		lmsUserDto.setExamResultDtoList(examResultDtoList);

		List<DailyReportDto> dailyReportDtoList = tDailyReportSubmitMapper
				.getDailyReportSubmitList(lmsUserId, Constants.DB_FLG_FALSE);
		lmsUserDto.setDailyReportDtoList(dailyReportDtoList);

		return lmsUserDto;
	}

	/**
	 * ユーザー基本情報（初期表示/検索用）
	 * @author VU HONG DUC_Task57
	 * @param courseName
	 * @param companyName
	 * @param userName
	 * @return userDetailDto
	 */
	public List<UserDetailDto> getuserDetailForSearch(String courseName, String companyName, String userName) {
		//会場IDリスト取得
		List<MPlace> placeList = mPlaceMapper.findByPlaceId(loginUserDto.getPlaceId(), Constants.DB_HIDDEN_FLG_FALSE,
				Constants.DB_FLG_FALSE);
		//会場IDリストを変換
		List<Integer> placeIdList = placeList.stream()
				.map(MPlace::getPlaceId)
				.collect(Collectors.toList());
		//ユーザー情報取得
		List<UserDetailDto> userDetailDto = mLmsUserMapper.getUserDetailForSearch(courseName, companyName, userName,
				placeIdList, Constants.CODE_VAL_ROLL_STUDENT, Constants.DB_FLG_FALSE);
		return userDetailDto;
	}
	/**
	 * 会場情報リスト取得
	 * @author VU HONG DUC_Task.57
	 * @param placeId
	 * @param deleteFlg
	 * @param hiddenFlg
	 * @return 会場情報リスト
	 */
	public List<MPlace> findPlaceByPlaceId(Integer placeId, Short deleteFlg, Short hiddenFlg) {
		//会場IDリスト取得
		List<MPlace> placeList = mPlaceMapper.findByPlaceId(loginUserDto.getPlaceId(), deleteFlg,hiddenFlg);
		return placeList;
	}
	/**
	 * 企業情報DTOリストの取得
	 * @author VU HONG DUC_Task.57
	 * @param deleteFlg
	 * @return	企業リスト
	 */
	public List<CompanyDto> getCompanyDto(Short deleteFlg){
		List<CompanyDto> companyDto = mCompanyMapper.getCompanyDto(deleteFlg);
		return companyDto;
	}
}
