package jp.co.sss.lms.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.entity.MPlace;

@Mapper
public interface MPlaceMapper {
	/**
	 * 会場情報（会場ID）取得
	 * @author VU HONG DUC_Task57
	 * @param placeId
	 * @param hiddenFlg
	 * @param deleteFlg
	 * @return 会場ID
	 */
	List<MPlace> findByPlaceId(@Param("placeId") Integer placeId, @Param("hiddenFlg") Short hiddenFlg,
			@Param("deleteFlg") Short deleteFlg);
}
