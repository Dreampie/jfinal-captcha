package cn.dreampie.captcha;

import cn.dreampie.captcha.color.ColorFactory;
import cn.dreampie.captcha.filter.ConfigurableFilterFactory;
import cn.dreampie.captcha.filter.library.WobbleImageOp;
import cn.dreampie.captcha.font.RandomFontFactory;
import cn.dreampie.captcha.service.Captcha;
import cn.dreampie.captcha.service.ConfigurableCaptchaService;
import cn.dreampie.captcha.text.renderer.BestFitTextRenderer;
import cn.dreampie.captcha.text.renderer.TextRenderer;
import cn.dreampie.captcha.word.RandomWordFactory;
import cn.dreampie.encription.EncriptionKit;
import com.jfinal.render.Render;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by wangrenhui on 13-12-31.
 */
public class CaptchaRender extends Render {

  private Logger logger = LoggerFactory.getLogger(getClass());
  private static final String CODE_CHAR = "0123456789";
  private static final int MIN_NUM = 4;
  private static final int MAX_NUM = 4;
  private static final int FONT_MIN_SIZE = 20;
  private static final int FONT_MAX_SIZE = 20;
  private static final double X_AMPLITUDE = 1.6;
  private static final double Y_AMPLITUDE = 0.8;
  private static final int TOP_MARGIN = 1;
  private static final int BOTTOM_MARGIN = 1;
  private static final int WIDTH = 118;
  private static final int HEIGHT = 41;

  private String captchaName = "captcha";
  private float alpha = 1.0f;
  private ConfigurableCaptchaService configurableCaptchaService = null;
  private ColorFactory colorFactory = null;
  private RandomFontFactory fontFactory = null;
  private RandomWordFactory wordFactory = null;
  private TextRenderer textRenderer = null;

  public CaptchaRender() {
    this(MIN_NUM, MAX_NUM, WIDTH, HEIGHT, FONT_MIN_SIZE, FONT_MAX_SIZE, null);
  }

  public CaptchaRender(int num) {
    this(num, num, WIDTH, HEIGHT, FONT_MIN_SIZE, FONT_MAX_SIZE, null);
  }

  public CaptchaRender(int minnum, int maxnum, int width, int height, int fontsize) {
    this(minnum, maxnum, width, height, fontsize, fontsize, null);
  }

  public CaptchaRender(int minnum, int maxnum, int width, int height, int fontsize, String code) {
    this(minnum, maxnum, width, height, fontsize, fontsize, code);
  }

  public CaptchaRender(int minnum, int maxnum, int width, int height, int fontmin, int fontmax, String code) {
    if (minnum <= 0) {
      minnum = MIN_NUM;
    }
    if (maxnum <= 0) {
      maxnum = MAX_NUM;
    }
    if (width <= 0) {
      width = WIDTH;
    }
    if (height <= 0) {
      height = HEIGHT;
    }

    if (fontmin <= 0) {
      fontmin = FONT_MIN_SIZE;
    }

    if (fontmax <= 0) {
      fontmax = FONT_MAX_SIZE;
    }

    if (!(code != null && !code.isEmpty())) {
      code = CODE_CHAR;
    }

    configurableCaptchaService = new ConfigurableCaptchaService();

    // 颜色创建工厂,使用一定范围内的随机色
    //colorFactory = new RandomColorFactory();
    colorFactory = new ColorFactory() {

      public Color getColor(int index) {
        return new Color(0, 0, 0);//new Color(118,102,102);
      }
    };

    configurableCaptchaService.setColorFactory(colorFactory);

    // 随机字体生成器
    fontFactory = new RandomFontFactory();
    fontFactory.setMaxSize(fontmin);
    fontFactory.setMinSize(fontmax);
    configurableCaptchaService.setFontFactory(fontFactory);

    // 随机字符生成器,去除掉容易混淆的字母和数字,如o和0等
    wordFactory = new RandomWordFactory();
    wordFactory.setCharacters(code);
    wordFactory.setMaxLength(maxnum);
    wordFactory.setMinLength(minnum);
    configurableCaptchaService.setWordFactory(wordFactory);

    // 自定义验证码图片背景
    SimpleBackgroundFactory backgroundFactory = new SimpleBackgroundFactory(alpha);
    configurableCaptchaService.setBackgroundFactory(backgroundFactory);

    // 图片滤镜设置
    ConfigurableFilterFactory filterFactory = new ConfigurableFilterFactory();

    java.util.List<BufferedImageOp> filters = new ArrayList<BufferedImageOp>();
    WobbleImageOp wobbleImageOp = new WobbleImageOp();
    wobbleImageOp.setEdgeMode(BufferedImage.TYPE_INT_ARGB);
    wobbleImageOp.setxAmplitude(X_AMPLITUDE);
    wobbleImageOp.setyAmplitude(Y_AMPLITUDE);
    filters.add(wobbleImageOp);
    filterFactory.setFilters(filters);

    configurableCaptchaService.setFilterFactory(filterFactory);

    // 文字渲染器设置
    textRenderer = new BestFitTextRenderer();
    textRenderer.setBottomMargin(BOTTOM_MARGIN);
    textRenderer.setTopMargin(TOP_MARGIN);
    configurableCaptchaService.setTextRenderer(textRenderer);

    // 验证码图片的大小
    configurableCaptchaService.setWidth(width);
    configurableCaptchaService.setHeight(height);
  }

  /**
   * you can  rewrite this  render
   * 输出
   */
  public void render() {
    ServletOutputStream outputStream = null;

    // 得到验证码对象,有验证码图片和验证码字符串
    Captcha captcha = configurableCaptchaService.getCaptcha();
    // 取得验证码字符串放入Session
    String captchaCode = captcha.getChallenge();
    if (logger.isDebugEnabled()) {
      logger.debug("captcha:" + captchaCode);
    }
    //System.out.println(validationCode);
    HttpSession session= request.getSession();
    session.setAttribute(captchaName, EncriptionKit.encrypt(captchaCode));
    session.setAttribute(captchaName + "_time", new Date().getTime());
//    CookieUtils.addCookie(request, response, AppConstants.CAPTCHA_NAME, EncriptionKit.encrypt(captchaCode), -1);
    // 取得验证码图片并输出
    BufferedImage bufferedImage = captcha.getImage();

    try {
      outputStream = response.getOutputStream();
      ImageIO.write(bufferedImage, "png", outputStream);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (outputStream != null)
        try {
          outputStream.flush();
          outputStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
    }

  }

  public String getCaptchaName() {
    return captchaName;
  }

  public void setCaptchaName(String captchaName) {
    this.captchaName = captchaName;
  }

  public float getAlpha() {
    return alpha;
  }

  public void setAlpha(float alpha) {
    this.alpha = alpha;
  }
}
