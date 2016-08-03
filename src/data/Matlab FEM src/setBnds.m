function [Kbnd, Fbnd] = setBnds(pts, bnd, bndHt, K, F, ht, nBnd, bndFunc)
% SET DIRCHLET BOUNDARY CONDITIONS
% pts : list of points in mesh
% bnd : list of idx's in pts comprising boundaries
% K : stiffness matrix from finite element calculation
% F : force vector
% nBnd : list of idx's corresponing to boundary neighbors
% bndFunc : functiond describing boundary state (symbolic)

%     if(bndFunc == 1)      %1 == neumann boundaries - determine gradient at boundary
%                  
%         %K(bnd,bnd)=speye(length(bnd),length(bnd));  % put Ident into bnd node submatrix of K
%         %K(bnd(1), bnd(1)) = .5;
%         %Npts=size(bnd,1);
%         %K(bnd(Npts-1), bnd(Npts-1)) = .5;
%         Kbnd=K;                                     
%         Fbnd=F;                                
% 
%     else                    %0 == dirichlet boundaries - determine value at boundary
        K(bnd,:)=0; 
        K(:,bnd)=0; 
        F(bnd)=bndHt;                  

        K(bnd,bnd) = speye(length(bnd),length(bnd));  % put Ident into bnd node submatrix of K
        Kbnd=K;                                     
        Fbnd=F;                            
%     end;
end