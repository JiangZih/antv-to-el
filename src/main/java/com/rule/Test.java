package com.rule;

import com.rule.parser.Flow;
import com.rule.parser.RuleParser;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ClassPathResource;

public class Test {

	/**
	 * �������
	 * el�е�data base64���ܣ����ڻ��е�����
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		ClassPathResource classPathResource = new ClassPathResource("dag.json");
		String rule = IoUtil.readUtf8(classPathResource.getStream());
		RuleParser parser = new RuleParser();
		//����dag��Flow����
		Flow flow = parser.parseFlow(rule);
		//����Flow��������EL
		String el = parser.genEL();
		System.out.println(el);
	}

}
