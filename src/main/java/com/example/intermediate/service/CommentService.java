package com.example.intermediate.service;

import com.example.intermediate.controller.request.CommentRequestDto;
import com.example.intermediate.controller.response.CommentResponseDto;
import com.example.intermediate.controller.response.ResponseDto;
import com.example.intermediate.controller.response.SubCommentResponeDto;
import com.example.intermediate.domain.Comment;
import com.example.intermediate.domain.Member;
import com.example.intermediate.domain.Post;
import com.example.intermediate.domain.SubComment;
import com.example.intermediate.domain.likes.CommentLikes;
import com.example.intermediate.jwt.TokenProvider;
import com.example.intermediate.repository.CommentLikeRepository;
import com.example.intermediate.repository.CommentRepository;
import com.example.intermediate.repository.SubCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final SubCommentRepository subCommentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final TokenProvider tokenProvider;
    private final PostService postService;

    @Transactional
    public ResponseDto<?> createComment(CommentRequestDto requestDto, HttpServletRequest request) {
        if (null == request.getHeader("Refresh-Token")) {
            return ResponseDto.fail("MEMBER_NOT_FOUND",
                    "로그인이 필요합니다.");
        }

        if (null == request.getHeader("Authorization")) {
            return ResponseDto.fail("MEMBER_NOT_FOUND",
                    "로그인이 필요합니다.");
        }

        Member member = validateMember(request);
        if (null == member) {
            return ResponseDto.fail("INVALID_TOKEN", "Token이 유효하지 않습니다.");
        }

        Post post = postService.isPresentPost(requestDto.getPostId());
        if (null == post) {
            return ResponseDto.fail("NOT_FOUND", "존재하지 않는 게시글 id 입니다.");
        }

        Comment comment = Comment.builder()
                .member(member)
                .post(post)
                .content(requestDto.getContent())
                .build();
        commentRepository.save(comment);
        return ResponseDto.success(
                CommentResponseDto.builder()
                        .id(comment.getId())
                        .author(comment.getMember().getNickname())
                        .content(comment.getContent())
                        .like(comment.getLikes())
                        .createdAt(comment.getCreatedAt())
                        .modifiedAt(comment.getModifiedAt())
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public ResponseDto<?> getAllCommentsByPost(Long postId) {
        Post post = postService.isPresentPost(postId);
        if (null == post) {
            return ResponseDto.fail("NOT_FOUND", "존재하지 않는 게시글 id 입니다.");
        }

        List<Comment> commentList = commentRepository.findAllByPost(post);
        List<CommentResponseDto> commentResponseDtoList = new ArrayList<>();


        for (Comment comment : commentList) {
            List<SubComment> subComments = subCommentRepository.findAllByComment(comment);
            List<SubCommentResponeDto> subCommentResponeDtos = new ArrayList<>();
            for (SubComment subcomment : subComments) {
                subCommentResponeDtos.add(
                        SubCommentResponeDto.builder()
                                .id(subcomment.getId())
                                .author(subcomment.getMember().getNickname())
                                .content(subcomment.getContent())
                                .like(subcomment.getLikes())
                                .createdAt(subcomment.getCreatedAt())
                                .modifiedAt(subcomment.getModifiedAt())
                                .build()
                );
            }
            commentResponseDtoList.add(
                    CommentResponseDto.builder()
                            .id(comment.getId())
                            .author(comment.getMember().getNickname())
                            .content(comment.getContent())
                            .like(comment.getLikes())
                            .Comments(subCommentResponeDtos)
                            .createdAt(comment.getCreatedAt())
                            .modifiedAt(comment.getModifiedAt())
                            .build()
            );
        }
        return ResponseDto.success(commentResponseDtoList);
    }

    @Transactional
    public ResponseDto<?> updateComment(Long id, CommentRequestDto requestDto, HttpServletRequest request) {
        if (null == request.getHeader("Refresh-Token")) {
            return ResponseDto.fail("MEMBER_NOT_FOUND",
                    "로그인이 필요합니다.");
        }

        if (null == request.getHeader("Authorization")) {
            return ResponseDto.fail("MEMBER_NOT_FOUND",
                    "로그인이 필요합니다.");
        }

        Member member = validateMember(request);
        if (null == member) {
            return ResponseDto.fail("INVALID_TOKEN", "Token이 유효하지 않습니다.");
        }

        Post post = postService.isPresentPost(requestDto.getPostId());
        if (null == post) {
            return ResponseDto.fail("NOT_FOUND", "존재하지 않는 게시글 id 입니다.");
        }

        Comment comment = isPresentComment(id);
        if (null == comment) {
            return ResponseDto.fail("NOT_FOUND", "존재하지 않는 댓글 id 입니다.");
        }

        if (comment.validateMember(member)) {
            return ResponseDto.fail("BAD_REQUEST", "작성자만 수정할 수 있습니다.");
        }

        List<SubComment> subComments = subCommentRepository.findAllByComment(comment);
        List<SubCommentResponeDto> subCommentResponeDtos = new ArrayList<>();
        for (SubComment subcomment : subComments) {
            subCommentResponeDtos.add(
                    SubCommentResponeDto.builder()
                            .id(subcomment.getId())
                            .author(subcomment.getMember().getNickname())
                            .content(subcomment.getContent())
                            .like(subcomment.getLikes())
                            .createdAt(subcomment.getCreatedAt())
                            .modifiedAt(subcomment.getModifiedAt())
                            .build()
            );
        }
        comment.update(requestDto);
        return ResponseDto.success(
                CommentResponseDto.builder()
                        .id(comment.getId())
                        .author(comment.getMember().getNickname())
                        .content(comment.getContent())
                        .like(comment.getLikes())
                        .Comments(subCommentResponeDtos)
                        .createdAt(comment.getCreatedAt())
                        .modifiedAt(comment.getModifiedAt())
                        .build()
        );
    }

    @Transactional
    public ResponseDto<?> deleteComment(Long id, HttpServletRequest request) {
        if (null == request.getHeader("Refresh-Token")) {
            return ResponseDto.fail("MEMBER_NOT_FOUND",
                    "로그인이 필요합니다.");
        }

        if (null == request.getHeader("Authorization")) {
            return ResponseDto.fail("MEMBER_NOT_FOUND",
                    "로그인이 필요합니다.");
        }

        Member member = validateMember(request);
        if (null == member) {
            return ResponseDto.fail("INVALID_TOKEN", "Token이 유효하지 않습니다.");
        }

        Comment comment = isPresentComment(id);
        if (null == comment) {
            return ResponseDto.fail("NOT_FOUND", "존재하지 않는 댓글 id 입니다.");
        }

        if (comment.validateMember(member)) {
            return ResponseDto.fail("BAD_REQUEST", "작성자만 수정할 수 있습니다.");
        }

        commentRepository.delete(comment);
        subCommentRepository.deleteAllByComment(comment);
        return ResponseDto.success("success");
    }

    @Transactional(readOnly = true)
    public Comment isPresentComment(Long id) {
        Optional<Comment> optionalComment = commentRepository.findById(id);
        return optionalComment.orElse(null);
    }

    @Transactional
    public Member validateMember(HttpServletRequest request) {
        if (!tokenProvider.validateToken(request.getHeader("Refresh-Token"))) {
            return null;
        }
        return tokenProvider.getMemberFromAuthentication();
    }

    public CommentLikes isPresentcommentLikes(Long Id, String nickname) {
        Optional<CommentLikes> optionalCommentLikes = commentLikeRepository.findByRequestIdAndNickname(Id, nickname);
        return optionalCommentLikes.orElse(null);
    }

    @Transactional
    public ResponseDto<?> createcommentlikes(Long id, HttpServletRequest request) {
        if (null == request.getHeader("Refresh-Token")) {
            return ResponseDto.fail("MEMBER_NOT_FOUND",
                    "로그인이 필요합니다.");
        }

        if (null == request.getHeader("Authorization")) {
            return ResponseDto.fail("MEMBER_NOT_FOUND",
                    "로그인이 필요합니다.");
        }

        Member member = validateMember(request);
        if (null == member) {
            return ResponseDto.fail("INVALID_TOKEN", "Token이 유효하지 않습니다.");
        }

        Comment comment = isPresentComment(id);
        if (null == comment) {
            return ResponseDto.fail("NOT_FOUND", "존재하지 않는 게시글 id 입니다.");
        }

        CommentLikes likes = isPresentcommentLikes(comment.getId(), member.getNickname());

        if (null == likes)
            commentLikeRepository.save(CommentLikes.builder()
                            .requestId(comment.getId())
                            .nickname(member.getNickname())
                            .build());
        else
            commentLikeRepository.delete(likes);

        comment.updatelikes(commentLikeRepository.findAllByRequestId(comment.getId()).size());

        return ResponseDto.success("like success");

    }


}
