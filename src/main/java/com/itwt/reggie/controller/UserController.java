package com.itwt.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itwt.reggie.common.R;
import com.itwt.reggie.entity.User;
import com.itwt.reggie.service.UserService;
import com.itwt.reggie.utils.MailUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService usesrService;
    @Autowired
    private RedisTemplate redisTemplate;


    /*
     * 发送邮件验证码
     * */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session) throws MessagingException {
        String phone = user.getPhone();
        if (!phone.isEmpty()) {
            //随机生成验证码
            String code = MailUtils.achieveCode();
            log.info(code);
            //phone就是邮箱，code是验证码
            MailUtils.sendTestMail(phone, code);
            //验证码存在Session,方便后面拿出来进行比对
            //session.setAttribute(phone,code);
            //验证码缓存到Redis，设置存活时间5分钟
            redisTemplate.opsForValue().set(phone, code, 5, TimeUnit.MINUTES);
            return R.success("验证码发送成功");
        }
        return R.error("验证码发送失败");
    }

    /*
     * 移动端用户登录，邮件版
     * */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session) {
        log.info(map.toString());
        //获取邮箱
        String phone = map.get("phone").toString();
        //获取验证码
        String code = map.get("code").toString();
        //从Session中获取验证码
        //String codeInSession = session.getAttribute(phone).toString();
        //把Redis缓存的code拿出来
        Object codeInRedis = redisTemplate.opsForValue().get(phone);
        //查看接收到用户输入的验证码是否和redis中的验证码是否相同
        //log.info("你输入的code{}，session中的code{}，计算结果为{}", code, codeInSession, (code != null && code.equals(codeInSession)));
        //看看接收到用户输入的验证码是否和redis中的验证码相同
        log.info("你输入的code{}，redis中的code{}，计算结果为{}", code, codeInRedis, (code != null && code.equals(codeInRedis)));
        //比较这个用户输入的验证码和Session中的是否一致
        //if (code!=null && code.equals(codeInSession)){
        if (code != null && code.equals(codeInRedis)) {
            //如果正确，判断用户是否存在
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            //判断依据是数据库中是否存在邮箱
            queryWrapper.eq(User::getPhone, phone);
            User user = usesrService.getOne(queryWrapper);
            //如果不存在，创建新用户并保存
            if (user == null) {
                user = new User();
                user.setPhone(phone);
                usesrService.save(user);
                //user.setName("用户"+codeInSession);
                user.setName("用户" + codeInRedis);
            }
            //存个Session表示登录状态
            session.setAttribute("user", user.getId());
            //如果登陆成功，则删除Redis中的验证码
            redisTemplate.delete(phone);
            //返回结果
            return R.success(user);
    }
        return R.error("登录失败");
}

    /*
     * 退出
     * */
    @PutMapping("/loginout")
    public R<String> loginout(HttpServletRequest request) {
        request.getSession().removeAttribute("user");
        return R.success("退出成功");
    }
    /* *//*
 * 发送验证码短信
 * *//*
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session) {
        //获取手机号
        String phone = user.getPhone();

        if (StringUtils.isNotEmpty(phone)) {
            //生成4位验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            //调用阿里云提供的短信服务API完成发送短信
            SMSUtils.sendMessage("瑞吉外卖", "", phone, code);
            //保存验证码到Session
            session.setAttribute(phone, code);
            return R.success("手机验证码发送成功");
        }
        return R.error("发送失败");
    }*/



    /* *//*
 * 移动端用户登录
 * *//*
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session) {
        //获取手机号
        String phone = map.get("phone").toString();
        //获取验证码
        String code = map.get("code").toString();
        //从Session中获取保存的验证码
        Object codeInSession = session.getAttribute(phone);
        //进行验证码的比对
        if (codeInSession != null && codeInSession.equals(code)) {
            //比对成功说明登录成功

            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone, phone);

            User user = usesrService.getOne(queryWrapper);

            if (user == null) {
                //判断当前手机号对应的用户是否为新用户，如果是就自动完成注册
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                usesrService.save(user);
            }
            session.setAttribute("user",user.getId());
            return R.success(user);
        }
        return R.error("登录失败");
    }*/


}
