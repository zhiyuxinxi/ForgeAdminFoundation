package com.mdframe.forge.plugin.message.service.impl;

import com.mdframe.forge.starter.core.i18n.UiMessages;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageTemplate;
import com.mdframe.forge.plugin.message.mapper.SysMessageTemplateMapper;
import com.mdframe.forge.plugin.message.service.MessageTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 消息模板服务实现
 */
@Slf4j
@Service
public class MessageTemplateServiceImpl implements MessageTemplateService {

    private final SysMessageTemplateMapper templateMapper;

    public MessageTemplateServiceImpl(SysMessageTemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(SysMessageTemplate template) {
        // 检查模板编码是否已存在
        Long count = templateMapper.countByCode(template.getTemplateCode(), null);
        if (count > 0) {
            throw new RuntimeException(UiMessages.translate("模板编码已存在：") + template.getTemplateCode());
        }
        templateMapper.insert(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysMessageTemplate template) {
        SysMessageTemplate existing = templateMapper.selectById(template.getId());
        if (existing == null) {
            throw new RuntimeException("模板不存在");
        }
        
        // 如果修改了模板编码，检查新编码是否已被使用
        if (!existing.getTemplateCode().equals(template.getTemplateCode())) {
            Long count = templateMapper.countByCode(template.getTemplateCode(), template.getId());
            if (count > 0) {
                throw new RuntimeException(UiMessages.translate("模板编码已存在：") + template.getTemplateCode());
            }
        }
        
        templateMapper.updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        templateMapper.deleteById(id);
    }

    @Override
    public SysMessageTemplate getById(Long id) {
        return templateMapper.selectById(id);
    }

    @Override
    public SysMessageTemplate getByCode(String templateCode) {
        return templateMapper.selectByCode(templateCode);
    }

    @Override
    public IPage<SysMessageTemplate> page(String type, String keyword, Integer pageNum, Integer pageSize) {
        return templateMapper.selectTemplatePage(new Page<>(pageNum, pageSize), type, keyword);
    }
}
