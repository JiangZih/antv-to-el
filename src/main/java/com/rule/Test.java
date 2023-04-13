package com.rule;

import com.rule.parser.Flow;
import com.rule.parser.RuleParser;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ClassPathResource;

public class Test {

	/**
	 * 测试入口
	 * el中的data base64加密，存在换行的问题
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		ClassPathResource classPathResource = new ClassPathResource("dag.json");
		String rule = IoUtil.readUtf8(classPathResource.getStream());
		RuleParser parser = new RuleParser();
		//解析dag到Flow对象
		Flow flow = parser.parseFlow(rule);
		//根据Flow对象生成EL
		String el = parser.genEL();
		System.out.println(el);
	}

}
