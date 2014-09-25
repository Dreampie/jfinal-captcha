package cn.dreampie.captcha;

import cn.dreampie.captcha.background.BackgroundFactory;
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by wangrenhui on 13-12-31.
 */
public class CaptchaRender extends Render {

  private Logger logger = LoggerFactory.getLogger(getClass());
  private String code = "0123456789";
  private int font_min_num = 4;
  private int font_max_num = 4;
  private int font_min_size = 20;
  private int font_max_size = 20;
  private double x_amplitude = 1.6;
  private double y_amplitude = 0.8;
  private int top_margin = 1;
  private int bottom_margin = 1;
  private int width = 118;
  private int height = 41;

  private String captchaName = "captcha";
  private ConfigurableCaptchaService configurableCaptchaService = null;
  private ColorFactory colorFactory = null;
  private RandomFontFactory fontFactory = null;
  private RandomWordFactory wordFactory = null;
  private TextRenderer textRenderer = null;
  private BackgroundFactory backgroundFactory = null;

  /**
   * 背景色
   */
  private Color bgColor = null;

  /**
   * 验证码字符颜色
   */
  private Color drawColor = new Color(0, 0, 0);
  /**
   * 背景元素的颜色
   */
  private Color drawBgColor = new Color(102, 102, 102);
  /**
   * 噪点数量
   */
  private int artifactNum = 50;

  private int lineNum=2;

  private void initCaptchService() {
    configurableCaptchaService = new ConfigurableCaptchaService();

    // 颜色创建工厂,使用一定范围内的随机色
    //colorFactory = new RandomColorFactory();
    colorFactory = new ColorFactory() {

      public Color getColor(int index) {
        return drawColor;//new Color(118,102,102);
      }
    };

    configurableCaptchaService.setColorFactory(colorFactory);

    // 随机字体生成器
    fontFactory = new RandomFontFactory();
    fontFactory.setMaxSize(font_max_size);
    fontFactory.setMinSize(font_min_size);
    configurableCaptchaService.setFontFactory(fontFactory);

    // 随机字符生成器,去除掉容易混淆的字母和数字,如o和0等
    wordFactory = new RandomWordFactory();
    wordFactory.setCharacters(code);
    wordFactory.setMaxLength(font_max_num);
    wordFactory.setMinLength(font_min_num);
    configurableCaptchaService.setWordFactory(wordFactory);

    // 自定义验证码图片背景
    if (backgroundFactory == null) {
      backgroundFactory = new SimpleBackgroundFactory(bgColor, drawBgColor, artifactNum,lineNum);
    }
    configurableCaptchaService.setBackgroundFactory(backgroundFactory);

    // 图片滤镜设置
    ConfigurableFilterFactory filterFactory = new ConfigurableFilterFactory();

    java.util.List<BufferedImageOp> filters = new ArrayList<BufferedImageOp>();
    WobbleImageOp wobbleImageOp = new WobbleImageOp();
    wobbleImageOp.setEdgeMode(BufferedImage.TYPE_INT_ARGB);
    wobbleImageOp.setxAmplitude(x_amplitude);
    wobbleImageOp.setyAmplitude(y_amplitude);
    filters.add(wobbleImageOp);
    filterFactory.setFilters(filters);

    configurableCaptchaService.setFilterFactory(filterFactory);

    // 文字渲染器设置
    textRenderer = new BestFitTextRenderer();
    textRenderer.setBottomMargin(bottom_margin);
    textRenderer.setTopMargin(top_margin);
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
    //初始化
    initCaptchService();
    ServletOutputStream outputStream = null;

    // 得到验证码对象,有验证码图片和验证码字符串
    Captcha captcha = configurableCaptchaService.getCaptcha();
    // 取得验证码字符串放入Session
    String captchaCode = captcha.getChallenge();
    if (logger.isDebugEnabled()) {
      logger.debug("captcha:" + captchaCode);
    }
    //System.out.println(validationCode);
    HttpSession session = request.getSession();
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

  public BackgroundFactory getBackgroundFactory() {
    return backgroundFactory;
  }

  public void setBackgroundFactory(BackgroundFactory backgroundFactory) {
    this.backgroundFactory = backgroundFactory;
  }

  public Color getDrawColor() {
    return drawColor;
  }

  public void setDrawColor(Color drawColor) {
    this.drawColor = drawColor;
  }

  public Color getDrawBgColor() {
    return drawBgColor;
  }

  public void setDrawBgColor(Color drawBgColor) {
    this.drawBgColor = drawBgColor;
  }

  public Color getBgColor() {
    return bgColor;
  }

  public void setBgColor(Color bgColor) {
    this.bgColor = bgColor;
  }

  public void setFontNum(int font_min_num, int font_max_num) {
    this.font_min_num = font_min_num;
    this.font_max_num = font_max_num;
  }


  public void setFontSize(int font_min_size, int font_max_size) {
    this.font_min_size = font_min_size;
    this.font_max_size = font_max_size;
  }

  public void setFontMargin(int top_margin, int bottom_margin) {
    this.top_margin = top_margin;
    this.bottom_margin = bottom_margin;
  }

  public void setImgSize(int width, int height) {
    this.width = width;
    this.height = height;
  }

  public void setArtifactNum(int artifactNum) {
    this.artifactNum = artifactNum;
  }

  public void setLineNum(int lineNum) {
    this.lineNum = lineNum;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public static void main(String[] args) {
    int imgWidth = 400;
    int imgHeight = 300;
    File file = new File("/home/ice/图片/test.png");
    BufferedImage image = new BufferedImage(imgWidth, imgHeight,
        BufferedImage.TYPE_INT_ARGB);//RGB形式
    BackgroundFactory bf = new SimpleBackgroundFactory(new Color(255, 255, 0, 100));
    bf.fillBackground(image);
    try {
      ImageIO.write(image, "png", file);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
