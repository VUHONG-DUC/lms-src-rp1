package jp.co.sss.lms.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.dto.CompanyDto;

/**
 * 企業マスターマッパー
 * @author VU HONG DUC_Task57
 */
@Mapper
public interface MCompanyMapper {
	/**
	 * 企業リストの取得
	 * @author VU HONG DUC_Task.57
	 * @param deleteFlg
	 * @return 企業リスト
	 */
	List<CompanyDto> getCompanyDto(@Param("deleteFlg") Short deleteFlg);
}
