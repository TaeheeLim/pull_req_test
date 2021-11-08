package kr.or.ddit.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import kr.or.ddit.service.ProductService;
import kr.or.ddit.util.FileuploadUtil;
import kr.or.ddit.vo.ProductVO;


@Controller
public class ProductController {
	
	@Autowired
	ProductService productService;
	private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

	@RequestMapping(value = "/products", method = RequestMethod.GET)
	public ModelAndView selectProducts(ModelAndView mav) {
		List<ProductVO> list = this.productService.selectProducts();
		
		mav.addObject("list", list);
		mav.setViewName("product/products");
		
		return mav;
	}

	@RequestMapping(value = "/product", method = RequestMethod.GET)
	public ModelAndView seeProduct(ModelAndView mav, @RequestParam String productId) {
		ProductVO vo = this.productService.selectProduct(productId);
		
		mav.addObject("vo", vo);
		mav.setViewName("product/product");

		return mav;
	}

	@RequestMapping(value = "/add", method = RequestMethod.GET)
	public ModelAndView addProduct(ModelAndView mav) {
		mav.setViewName("product/addProduct");
		return mav;
	}

	@PostMapping("/add")
	public String addProduct(ProductVO productVO, MultipartFile uploadFile) {

		productVO.setFilename(uploadFile.getOriginalFilename());
		productService.insertProduct(productVO, uploadFile);
		
		return "redirect:/products";
	}

	@RequestMapping(value = "/edit", method = RequestMethod.GET)
	public ModelAndView editProduct(ModelAndView mav, String edit) {

		List<ProductVO> list = productService.selectProducts();

		mav.addObject("list", list);
		mav.addObject("param", edit);
		mav.setViewName("product/editProduct");

		return mav;
	}

	@RequestMapping(value = "/update", method = RequestMethod.GET)
	public ModelAndView updateProduct(ModelAndView mav, String id) {
		ProductVO productVO = this.productService.selectProduct(id);
		mav.addObject("productVO", productVO);
		mav.setViewName("product/updateProduct");

		return mav;
	}

	@PostMapping("/update")
	public String updateProductForReal(ProductVO productVO, MultipartFile uploadFile) {

		productVO.setFilename(uploadFile.getOriginalFilename());

		if (!uploadFile.isEmpty()) {
			productService.updateProduct(productVO, uploadFile);
		} else {
			productService.updateProductNoFile(productVO);
		}

		return "redirect:/products";
	}

	@GetMapping("/delete")
	public String deleteProduct(String id) {
		productService.deleteProduct(id);

		return "redirect:/products";
	}

	@PostMapping("/addCart")
	public String addCart(String id, HttpServletRequest request, Model model) {
		logger.info("id : " + id);
		boolean addCart = productService.addCart(id, request);

		if (addCart) {
			logger.info(addCart + "");
			model.addAttribute("productId", id);
			return "redirect:/product";
		}
		return "redirect:/products";
	}

	@GetMapping("/cart")
	public String cart(Model model, HttpServletRequest request) {
		HttpSession session = request.getSession();
		List<ProductVO> list = (List<ProductVO>) session.getAttribute("cartlist");
		String id = session.getId();
		session.setAttribute("cartId", id);
		model.addAttribute("list", list);

		return "product/cart";
	}

	@GetMapping("/deleteCart")
	public String deleteCart(String cartId, HttpServletRequest request) {
		if (cartId == null || cartId.trim().equals("")) {
			return "product/cart";
		}
		HttpSession session = request.getSession();
		session.invalidate();
		return "product/cart";
	}

	@GetMapping("/shippingInfo")
	public String shippingInfo(String cartId, Model model) {
		model.addAttribute("cartId", cartId);

		return "product/shippingInfo";
	}

	@GetMapping("/checkOutCancelled")
	public String checkOutCancelled() {
		return "product/checkOutCancelled";
	}

	@PostMapping("/processShippingInfo")
	public String processShippingInfo(@RequestParam Map<String, String> map, HttpServletResponse response,
			HttpServletRequest request, Model model) {

		logger.info("주소이름" + map.get("addressName"));
		logger.info("국가이름" + map.get("country"));
		try {

			Cookie cartId = new Cookie("Shipping_cartId", URLEncoder.encode(map.get("cartId"), "UTF-8"));

			Cookie name = new Cookie("Shipping_name", URLEncoder.encode(map.get("name"), "UTF-8"));

			Cookie shippingDate = new Cookie("Shipping_shippingDate",
					URLEncoder.encode(map.get("shippingDate"), "UTF-8"));

			Cookie country = new Cookie("Shipping_country", URLEncoder.encode(map.get("country"), "UTF-8"));

			Cookie zipCode = new Cookie("Shipping_zipCode", URLEncoder.encode(map.get("zipCode"), "UTF-8"));

			Cookie addressName = new Cookie("addressName", URLEncoder.encode(map.get("addressName"), "UTF-8"));

			cartId.setMaxAge(24 * 60 * 60);
			name.setMaxAge(24 * 60 * 60);
			shippingDate.setMaxAge(24 * 60 * 60);
			zipCode.setMaxAge(24 * 60 * 60);
			country.setMaxAge(24 * 60 * 60);
			addressName.setMaxAge(24 * 60 * 60);

			response.addCookie(cartId);
			response.addCookie(name);
			response.addCookie(shippingDate);
			response.addCookie(country);
			response.addCookie(zipCode);
			response.addCookie(addressName);
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "redirect:/exceptionNoProductId";
		}

		return "product/orderConfirmation";
	}


	@GetMapping("/exceptionNoProductId")
	public String exceptionNoProductId() {
		return "product/exceptionNoProductId";
	}
	
	@GetMapping("/thankCustomer")
	public String thankCustomer(HttpServletRequest request) {
		//세션에 담긴 장바구니 삭제
		HttpSession session = request.getSession();
		session.invalidate();
		//모든 쿠키 삭제
		Cookie[] cookies = request.getCookies();
		for(int i = 0; i < cookies.length; i++) {
			Cookie thisCookie = cookies[i];
			thisCookie.setMaxAge(0);
		}
		
		return "product/thankCustomer";
	}
	
	@RequestMapping(value = "/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadFile(@RequestHeader("User-Agent") String userAgent ,String fileName) {
       
       logger.info("download file : " + fileName);
       
       //파일이 있는 절대 경로
       String uploadFolder = FileuploadUtil.uploadFolder;
       
       // ...resources\\upload\\2021\\11\\05\\개똥이.jpg
       Resource resource = new FileSystemResource(uploadFolder  + "\\"+ fileName);
       
       //해당 파일이 없을때...
       if(!resource.exists()) {
          return new ResponseEntity<Resource>(HttpStatus.NOT_FOUND);
       }
       
       logger.info("resource => " + resource);
       
       //파일명 가져오기
       String resourceName = resource.getFilename();
       
       //파일명이 한글이면 httpHeaders 객체를 통해 처리
       HttpHeaders headers = new HttpHeaders();
       
       try {
          // 헤더의 파일이름 처리하기 전에 해줘야함
          String downloadName = null;
          // Trident => IE 11버전의 엔진이름, 즉 IE를 나타냄
          if(userAgent.contains("Trident")) {
             logger.info("IE browser");
             
             downloadName = URLEncoder.encode(resourceName, "UTF-8").replaceAll("\\+", " ");
          } else if(userAgent.contains("Edg")) {
             logger.info("Edge browser");
             
             downloadName = URLEncoder.encode(resourceName, "UTF-8");
          }else{
             logger.info("chrome browser");
             
             downloadName = new String(resourceName.getBytes("UTF-8"), "ISO-8859-1");
          }
          
          //Content-disposition : 다운로드시 저장되는 이름을 처리하라
          headers.add("Content-disposition", "attachment;filename="+ downloadName);
       } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
       }
       
       // resource : 첨부파일 객체
       // headers : 파일명 처리 정보
       // HttpStatus.OK : 200(성공)
       return new ResponseEntity<Resource>(resource, headers, HttpStatus.OK);
    }
	
	@GetMapping("/getAlarm")
	public String alarm() {
		logger.info("alarm on!");
		
		return "alarm/alarm";
	}
	
	@GetMapping("/welcome")
	public String welcome() {
		return "product/welcome";
	}
	
	@RequestMapping("/tiles/body1")
	public String titles1() {
		// */* 및 {1}/{2}
		return "test1/body1";
	}
	
	@RequestMapping("/tiles/body2")
	public String tiles2() {
		return "test2/body2";
	}
	
}


