package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.domain.vo.ReplyVO;

/**
 * <p>
 * 互动提问的问题表 服务类
 * </p>
 *
 * @author Etsuya
 * @since 2024-03-31
 */
public interface IInteractionQuestionService extends IService<InteractionQuestion> {

	void submitQuestion(QuestionFormDTO dto);

	void modifyQuestion(Long id, QuestionFormDTO dto);

	PageDTO<QuestionVO> queryQuestion(QuestionPageQuery dto);

	QuestionVO queryQuestionById(Long id);

	void deleteQuestion(Long id);

	PageDTO<QuestionAdminVO> adminPageQuery(QuestionAdminPageQuery queryDto);

	void setHidden(Long id, Boolean hidden);

	QuestionAdminVO questionAdminDetail(Long id);

}
