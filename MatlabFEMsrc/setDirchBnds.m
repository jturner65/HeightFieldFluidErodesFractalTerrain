function [Kbnd, Fbnc] = setDirchBnds(pts, bnd, K, F, ht, Npts, bndFunc)
% SET DIRCHLET BOUNDARY CONDITIONS
% pts : list of points in mesh
% bnd : list of idx's in pts comprising boundaries
% K : stiffness matrix from finite element calculation
% F : force vector
% bndFunc : functiond describing boundary state (symbolic)

    if(bndFunc == 'N')      %neumann boundaries - determine gradient at boundary
        K(bnd,:)=0; 
        K(:,bnd)=0; 
        F(bnd)=0;                  

        K(bnd,bnd)=speye(length(bnd),length(bnd));  % put Ident into bnd node submatrix of K
        Kbnd=K;                                     
        Fbnc=F;                                

    else                    %dirichlet boundaries - determine value at boundary
        K(bnd,:)=0; 
        K(:,bnd)=0; 
        F(bnd)=0;                  

        K(bnd,bnd) = speye(length(bnd),length(bnd));  % put Ident into bnd node submatrix of K
        Kbnd=K;                                     
        Fbnc=F;                            
    end;
end