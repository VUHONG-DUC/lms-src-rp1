package jp.co.sss.lms.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.dto.UserDetailDto;

/**
 * LMSユーザーマスタマッパー
 * 
 * @author 東京ITスクール
 */
@Mapper
public interface MLmsUserMapper {

	/**
	 * ユーザー基本情報取得
	 * 
	 * @param lmsUserId
	 * @param deleteFlg
	 * @return ユーザー基本情報DTO
	 */
	UserDetailDto getUserDetail(@Param("lmsUserId") Integer lmsUserId,
			@Param("deleteFlg") Short deleteFlg);

	/**
	 * ユーザー基本情報取得（検索用）
	 * @author VU HONG DUC_Task.57
	 * @param courseName
	 * @param companyName
	 * @param userName
	 * @param placeIdList
	 * @param role
	 * @param leaveFlg
	 * @param pastDate
	 * @param deteleFlg
	 * @return	ユーザー基本情報DTOリスト
	 */
	List<UserDetailDto> getUserDetailForSearch(@Param("courseName") String courseName,
			@Param("companyName") String companyName,
			@Param("userName") String userName,
			@Param("placeIdList") List<Integer> placeIdList,
			@Param("role") String role,
			@Param("deleteFlg") Short deteleFlg);
}
